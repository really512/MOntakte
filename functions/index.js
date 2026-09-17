const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { setGlobalOptions } = require('firebase-functions/v2');
const admin = require('firebase-admin');

admin.initializeApp();
setGlobalOptions({ region: 'europe-west1' });

async function sendToUser(uid, message) {
  const snap = await admin.firestore().collection('users').doc(uid).get();
  if (!snap.exists) return;
  const token = snap.get('fcmToken');
  if (!token) return;
  try {
    await admin.messaging().send({ token, ...message });
  } catch (e) {
    console.error('FCM send failed', e);
  }
}

async function suspendUser(uid, days, reason) {
  const until = new Date(Date.now() + days * 24 * 60 * 60 * 1000);
  try {
    await admin.auth().updateUser(uid, { disabled: true });
    await admin.firestore().collection('users').doc(uid).set({
      moderationSuspendedUntil: admin.firestore.Timestamp.fromDate(until),
      moderationReason: reason
    }, { merge: true });
  } catch (e) {
    console.error('Moderation suspension failed', e);
  }
}

// Content creators must pass moderation metadata from the trusted publishing flow.
// If adultContent is true, the content is removed and the account is suspended.
exports.moderateAdultPost = onDocumentCreated('posts/{postId}', async event => {
  const data = event.data?.data();
  if (!data?.adultContent || !data?.authorUid) return;

  await event.data.ref.delete();
  await suspendUser(data.authorUid, 3, 'adult_content_post');
});

exports.moderateAdultCommunityMessage = onDocumentCreated('communities/{communityId}/messages/{messageId}', async event => {
  const data = event.data?.data();
  if (!data?.adultContent || !data?.senderUid) return;

  const communityRef = admin.firestore().collection('communities').doc(event.params.communityId);
  const communitySnap = await communityRef.get();
  const community = communitySnap.data() || {};

  await event.data.ref.delete();

  if (community.type === 'channel') {
    // A channel containing prohibited adult content is permanently removed.
    await communityRef.delete();
    await suspendUser(data.senderUid, 5, 'adult_content_channel');
  } else {
    // Group messages with prohibited adult content are permanently removed.
    await suspendUser(data.senderUid, 3, 'adult_content_group_message');
  }
});

exports.restoreModerationSuspensions = onSchedule('every 15 minutes', async () => {
  const now = admin.firestore.Timestamp.now();
  const snap = await admin.firestore().collection('users')
    .where('moderationSuspendedUntil', '<=', now)
    .limit(100)
    .get();

  for (const doc of snap.docs) {
    try {
      await admin.auth().updateUser(doc.id, { disabled: false });
      await doc.ref.update({
        moderationSuspendedUntil: admin.firestore.FieldValue.delete(),
        moderationReason: admin.firestore.FieldValue.delete()
      });
    } catch (e) {
      console.error(`Failed to restore ${doc.id}`, e);
    }
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

  await sendToUser(receiverUid, {
    notification: { title: senderName, body: text },
    data: { type: 'message', conversationId: event.params.conversationId, senderUid: data.senderUid }
  });
});

exports.notifyIncomingCall = onDocumentCreated('calls/{callId}', async event => {
  const data = event.data?.data();
  if (!data?.receiverUid || !data?.callerUid) return;
  const caller = await admin.firestore().collection('users').doc(data.callerUid).get();
  const callerName = caller.get('displayName') || caller.get('phone') || 'Контакт';
  const video = data.type === 'video';

  await sendToUser(data.receiverUid, {
    notification: {
      title: video ? '📹 Видеозвонок' : '📞 Входящий звонок',
      body: `${callerName} звонит вам`
    },
    data: { type: 'call', callId: event.params.callId, callerUid: data.callerUid, callType: data.type || 'audio' },
    android: { priority: 'high' }
  });
});
