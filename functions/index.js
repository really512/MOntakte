const { onDocumentCreated } = require('firebase-functions/v2/firestore');
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
