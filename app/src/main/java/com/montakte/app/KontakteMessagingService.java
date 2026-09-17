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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;

public class KontakteMessagingService extends FirebaseMessagingService {
 private static final String CHAT_CHANNEL="chat_messages", CALL_CHANNEL="incoming_calls";
 @Override public void onCreate(){super.onCreate();createChannels();syncCurrentToken();}
 @Override public void onNewToken(@NonNull String token){super.onNewToken(token);saveToken(token);}
 private void syncCurrentToken(){FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::saveToken);}
 private void saveToken(String token){if(token==null||token.isEmpty())return;FirebaseAuth a=FirebaseAuth.getInstance();if(a.getCurrentUser()==null)return;Map<String,Object> d=new HashMap<>();d.put("fcmToken",token);d.put("fcmUpdatedAt",FieldValue.serverTimestamp());FirebaseFirestore.getInstance().collection("users").document(a.getCurrentUser().getUid()).set(d,SetOptions.merge());}
 @Override public void onMessageReceived(@NonNull RemoteMessage message){super.onMessageReceived(message);Map<String,String> data=message.getData();String type=data.get("type");if(type==null||type.isEmpty())type="chat";String title=data.get("title"),body=data.get("body");if(title==null||title.isEmpty())title="call".equals(type)?"📞 Входящий звонок":"💬 Новое сообщение";if(body==null||body.isEmpty())body="Откройте ВОнтакте";
  Intent intent;if("call".equals(type)){intent=new Intent(this,CallActivity.class);String callId=data.get("callId");String callerUid=data.get("callerUid");if(callId!=null)intent.putExtra("callId",callId);if(callerUid!=null)intent.putExtra("peerUid",callerUid);intent.putExtra("incoming",true);intent.putExtra("videoCall","video".equals(data.get("callType")));}else{intent=new Intent(this,MainActivity.class);if(data.get("conversationId")!=null)intent.putExtra("conversationId",data.get("conversationId"));intent.putExtra("notificationType",type);}intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  int id=(int)(System.currentTimeMillis()&0x7fffffff);PendingIntent pi=PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);int imp="call".equals(type)?NotificationManager.IMPORTANCE_HIGH:NotificationManager.IMPORTANCE_DEFAULT;String ch="call".equals(type)?CALL_CHANNEL:CHAT_CHANNEL;NotificationCompat.Builder b=new NotificationCompat.Builder(this,ch).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setStyle(new NotificationCompat.BigTextStyle().bigText(body)).setPriority(imp).setAutoCancel(!"call".equals(type)).setContentIntent(pi);if("call".equals(type))b.setOngoing(true).setCategory(NotificationCompat.CATEGORY_CALL).setTimeoutAfter(60000L);if(Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)NotificationManagerCompat.from(this).notify(id,b.build());}
 private void createChannels(){if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O)return;NotificationManager m=getSystemService(NotificationManager.class);NotificationChannel c=new NotificationChannel(CHAT_CHANNEL,"Сообщения",NotificationManager.IMPORTANCE_DEFAULT);c.setDescription("Чаты, группы и каналы");NotificationChannel v=new NotificationChannel(CALL_CHANNEL,"Звонки",NotificationManager.IMPORTANCE_HIGH);v.setDescription("Входящие аудио- и видеозвонки");m.createNotificationChannel(c);m.createNotificationChannel(v);}
}
