package com.montakte.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class KontakteMessagingService extends FirebaseMessagingService {
    private static final String CHAT_CHANNEL = "chat_messages";
    private static final String CALL_CHANNEL = "incoming_calls";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        data.put("fcmUpdatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String, String> data = message.getData();
        String type = data.get("type");
        if (type == null || type.isEmpty()) type = "chat";

        String title = data.get("title");
        String body = data.get("body");
        if (title == null || title.isEmpty()) {
            if ("call".equals(type)) title = "📞 Входящий звонок";
            else if ("group".equals(type)) title = "👥 Новое сообщение в группе";
            else if ("channel".equals(type)) title = "📢 Новое сообщение в канале";
            else title = "💬 Новое сообщение";
        }
        if (body == null || body.isEmpty()) body = "Откройте Контакте";

        Intent intent;
        if ("call".equals(type)) {
            intent = new Intent(this, CallActivity.class);
            String peerUid = data.get("peerUid");
            if (peerUid != null) intent.putExtra("peerUid", peerUid);
            intent.putExtra("videoCall", "video".equals(data.get("callType")));
        } else {
            intent = new Intent(this, MainActivity.class);
            String conversationId = data.get("conversationId");
            if (conversationId != null) intent.putExtra("conversationId", conversationId);
            intent.putExtra("notificationType", type);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int channel = "call".equals(type) ? android.app.NotificationManager.IMPORTANCE_HIGH : android.app.NotificationManager.IMPORTANCE_DEFAULT;
        String channelId = "call".equals(type) ? CALL_CHANNEL : CHAT_CHANNEL;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(channel)
                .setAutoCancel(!"call".equals(type))
                .setContentIntent(pendingIntent);

        if ("call".equals(type)) {
            builder.setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setTimeoutAfter(60000L);
        }

        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(requestCode, builder.build());
        }
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel chat = new NotificationChannel(CHAT_CHANNEL, "Сообщения", NotificationManager.IMPORTANCE_DEFAULT);
        chat.setDescription("Уведомления из чатов, групп и каналов");
        NotificationChannel calls = new NotificationChannel(CALL_CHANNEL, "Звонки", NotificationManager.IMPORTANCE_HIGH);
        calls.setDescription("Входящие аудио- и видеозвонки");
        manager.createNotificationChannel(chat);
        manager.createNotificationChannel(calls);
    }
}
