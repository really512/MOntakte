const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { setGlobalOptions } = require('firebase-functions/v2');
const { defineSecret } = require('firebase-functions/params');
const admin = require('firebase-admin');
admin.initializeApp();
setGlobalOptions({ region: 'europe-west1' });
const OPENAI_API_KEY = defineSecret('OPENAI_API_KEY');

async function sendToUser(uid, message) {
  const snap = await admin.firestore().collection('users').doc(uid).get();
  const token = snap.get('fcmToken');
  if (!token) return;
  try { await admin.messaging().send({ token, ...message }); }
  catch (e) { console.error('FCM send failed', e); }
}

async function suspendUser(uid, days, reason) {
  const until = new Date(Date.now() + days * 86400000);
  try {
    await admin.auth().updateUser(uid, { disabled: true });
    await admin.firestore().collection('users').doc(uid).set({ moderationSuspendedUntil: admin.firestore.Timestamp.fromDate(until), moderationReason: reason }, { merge: true });
  } catch (e) { console.error('Moderation suspension failed', e); }
}

function buildModerationInput(data) {
  const input = [];
  const text = typeof data?.text === 'string' ? data.text.trim() : '';
  if (text) input.push({ type: 'text', text });
  const mediaUrl = typeof data?.mediaUrl === 'string' ? data.mediaUrl.trim() : '';
  if (mediaUrl && /^https?:\/\//i.test(mediaUrl)) input.push({ type: 'image_url', image_url: { url: mediaUrl } });
  return input;
}

async function containsAdultContent(data, apiKey) {
  if (data?.adultContent === true) return true;
  const input = buildModerationInput(data);
  if (!input.length || !apiKey) return false;
  try {
    const response = await fetch('https://api.openai.com/v1/moderations', { method: 'POST', headers: { Authorization: `Bearer ${apiKey}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ model: 'omni-moderation-latest', input }) });
    if (!response.ok) { console.error('Moderation API failed:', response.status, await response.text()); return false; }
    const result = await response.json();
    return Boolean(result?.results?.some(item => item?.flagged || item?.categories?.sexual === true));
  } catch (e) { console.error('Automatic moderation failed:', e); return false; }
}

async function moderatePostData(ref, data, days, reason) {
  if (!data?.authorUid || !(await containsAdultContent(data, OPENAI_API_KEY.value()))) return false;
  await ref.delete();
  await suspendUser(data.authorUid, days, reason);
  return true;
}

exports.moderateAdultPost = onDocumentCreated({ document: 'posts/{postId}', secrets: [OPENAI_API_KEY] }, async event => {
  const data = event.data?.data();
  await moderatePostData(event.data.ref, data, 3, 'adult_content_post');
});

exports.moderateAdultComment = onDocumentCreated({ document: 'posts/{postId}/comments/{commentId}', secrets: [OPENAI_API_KEY] }, async event => {
  const data = event.data?.data();
  await moderatePostData(event.data.ref, data, 3, 'adult_content_comment');
});

exports.moderateAdultCommunityMessage = onDocumentCreated({ document: 'communities/{communityId}/messages/{messageId}', secrets: [OPENAI_API_KEY] }, async event => {
  const data = event.data?.data();
  if (!data?.senderUid || !(await containsAdultContent(data, OPENAI_API_KEY.value()))) return;
  const communityRef = admin.firestore().collection('communities').doc(event.params.communityId);
  const community = (await communityRef.get()).data() || {};
  await event.data.ref.delete();
  if (community.type === 'channel') {
    await admin.firestore().recursiveDelete(communityRef);
    await suspendUser(data.senderUid, 5, 'adult_content_channel');
  } else {
    await suspendUser(data.senderUid, 3, 'adult_content_group_message');
  }
});

exports.restoreModerationSuspensions = onSchedule('every 15 minutes', async () => {
  const now = admin.firestore.Timestamp.now();
  const snap = await admin.firestore().collection('users').where('moderationSuspendedUntil', '<=', now).limit(100).get();
  for (const doc of snap.docs) {
    try {
      await admin.auth().updateUser(doc.id, { disabled: false });
      await doc.ref.update({ moderationSuspendedUntil: admin.firestore.FieldValue.delete(), moderationReason: admin.firestore.FieldValue.delete() });
    } catch (e) { console.error(`Failed to restore ${doc.id}`, e); }
  }
});

exports.notifyNewMessage = onDocumentCreated('conversations/{conversationId}/messages/{messageId}', async event => {
  const data = event.data?.data();
  if (!data?.senderUid) return;
  const conversation = await admin.firestore().collection('conversations').doc(event.params.conversationId).get();
  const participants = conversation.get('participants') || [];
  const receiverUid = participants.find(uid => uid !== data.senderUid);
  if (!receiverUid) return;
  const sender = await admin.firestore().collection('users').doc(data.senderUid).get();
  const senderName = sender.get('displayName') || sender.get('phone') || 'Контакт';
  const text = String(data.text || 'Новое сообщение').slice(0, 120);
  await sendToUser(receiverUid, { notification: { title: senderName, body: text }, data: { type: 'message', conversationId: event.params.conversationId, senderUid: data.senderUid } });
});

exports.notifyIncomingCall = onDocumentCreated('calls/{callId}', async event => {
  const data = event.data?.data();
  if (!data?.receiverUid || !data?.callerUid) return;
  const caller = await admin.firestore().collection('users').doc(data.callerUid).get();
  const callerName = caller.get('displayName') || caller.get('phone') || 'Контакт';
  const video = data.type === 'video';
  await sendToUser(data.receiverUid, { notification: { title: video ? '📹 Видеозвонок' : '📞 Входящий звонок', body: `${callerName} звонит вам` }, data: { type: 'call', callId: event.params.callId, callerUid: data.callerUid, callType: data.type || 'audio' }, android: { priority: 'high' } });
});
