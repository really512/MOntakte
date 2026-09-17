package com.montakte.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import java.util.HashMap;
import java.util.Map;

public class CallActivity extends Activity {
 private static final int PERMISSIONS=700;
 private FirebaseFirestore db; private FirebaseAuth auth; private String peerUid,callId; private boolean videoCall,incoming; private PeerConnectionFactory factory; private AudioTrack audioTrack; private VideoTrack videoTrack; private CameraVideoCapturer cameraCapturer; private TextView status; private Button answer,reject,end;
 @Override protected void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();peerUid=getIntent().getStringExtra("peerUid");callId=getIntent().getStringExtra("callId");incoming=getIntent().getBooleanExtra("incoming",false);videoCall=getIntent().getBooleanExtra("videoCall",false);buildUi();if(callId!=null)loadCall();else requestPermissionsAndStart();}
 private void buildUi(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setPadding(28,28,28,28);status=new TextView(this);status.setTextSize(25);status.setGravity(Gravity.CENTER);status.setText(incoming?"📞 Входящий звонок":"📞 Звонок");r.addView(status);TextView p=new TextView(this);p.setTextSize(17);p.setGravity(Gravity.CENTER);p.setText(peerUid==null?"Собеседник":"Собеседник: "+peerUid);r.addView(p);answer=new Button(this);answer.setText(videoCall?"📹 Принять видеозвонок":"📞 Принять звонок");reject=new Button(this);reject.setText("✖ Отклонить");end=new Button(this);end.setText("❌ Завершить");if(incoming){r.addView(answer);r.addView(reject);}r.addView(end);answer.setOnClickListener(v->acceptCall());reject.setOnClickListener(v->finishCall("rejected"));end.setOnClickListener(v->finishCall("ended"));setContentView(r);}
 private void loadCall(){db.collection("calls").document(callId).get().addOnSuccessListener(d->{if(!d.exists()){toast("Звонок уже недоступен");finish();return;}String caller=d.getString("callerUid"),receiver=d.getString("receiverUid"),type=d.getString("type"),state=d.getString("status");if(auth.getCurrentUser()==null||(!auth.getCurrentUser().getUid().equals(caller)&&!auth.getCurrentUser().getUid().equals(receiver))){finish();return;}peerUid=auth.getCurrentUser().getUid().equals(caller)?receiver:caller;videoCall="video".equals(type);if(incoming&&"ringing".equals(state))return;if("ringing".equals(state)&&!incoming){requestPermissionsAndStart();}else if("accepted".equals(state)){requestPermissionsAndStart();}else{toast("Звонок завершён");finish();}}).addOnFailureListener(e->{toast("Не удалось открыть звонок");finish();});}
 private void acceptCall(){if(callId==null){requestPermissionsAndStart();return;}db.collection("calls").document(callId).update("status","accepted","answeredAt",FieldValue.serverTimestamp()).addOnSuccessListener(v->{incoming=false;answer.setVisibility(android.view.View.GONE);reject.setVisibility(android.view.View.GONE);requestPermissionsAndStart();}).addOnFailureListener(e->toast("Не удалось принять звонок: "+e.getMessage()));}
 private void requestPermissionsAndStart(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED||(videoCall&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)){ActivityCompat.requestPermissions(this,videoCall?new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA}:new String[]{Manifest.permission.RECORD_AUDIO},PERMISSIONS);}else startMediaAndCall();}
 @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(requestCode,p,g);if(requestCode==PERMISSIONS){boolean ok=g.length>0;for(int x:g)if(x!=PackageManager.PERMISSION_GRANTED)ok=false;if(ok)startMediaAndCall();else{Toast.makeText(this,"Нужен доступ к микрофону"+(videoCall?" и камере":""),Toast.LENGTH_LONG).show();finish();}}}
 private void startMediaAndCall(){try{PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());factory=PeerConnectionFactory.builder().createPeerConnectionFactory();AudioSource as=factory.createAudioSource(new MediaConstraints());audioTrack=factory.createAudioTrack("audio0",as);if(videoCall){VideoSource vs=factory.createVideoSource(false);cameraCapturer=createCameraCapturer();if(cameraCapturer!=null){cameraCapturer.initialize(null,getApplicationContext(),vs.getCapturerObserver());cameraCapturer.startCapture(640,480,30);videoTrack=factory.createVideoTrack("video0",vs);}}if(!incoming&&callId==null)createCallRecord();listenCall();status.setText(videoCall?"📹 Подключение…":"📞 Подключение…");}catch(Exception e){toast("Не удалось запустить звонок: "+e.getMessage());finish();}}
 private CameraVideoCapturer createCameraCapturer(){Camera2Enumerator e=new Camera2Enumerator(this);for(String n:e.getDeviceNames())if(e.isFrontFacing(n))return e.createCapturer(n,null);for(String n:e.getDeviceNames())return e.createCapturer(n,null);return null;}
 private void createCallRecord(){if(auth.getCurrentUser()==null||peerUid==null)return;Map<String,Object> c=new HashMap<>();c.put("callerUid",auth.getCurrentUser().getUid());c.put("receiverUid",peerUid);c.put("type",videoCall?"video":"audio");c.put("status","ringing");c.put("createdAt",FieldValue.serverTimestamp());db.collection("calls").add(c).addOnSuccessListener(r->{callId=r.getId();status.setText(videoCall?"📹 Ожидание ответа…":"📞 Ожидание ответа…");listenCall();});}
 private void listenCall(){if(callId==null)return;db.collection("calls").document(callId).addSnapshotListener((d,e)->{if(e!=null||d==null||!d.exists())return;String s=d.getString("status");if("accepted".equals(s)){status.setText(videoCall?"📹 Звонок активен":"📞 Звонок активен");}else if("rejected".equals(s)||"ended".equals(s)){toast("Звонок завершён");finish();}});}
 private void finishCall(String state){if(callId!=null)db.collection("calls").document(callId).update("status",state,"endedAt",FieldValue.serverTimestamp());stopMedia();finish();}
 private void stopMedia(){if(cameraCapturer!=null)try{cameraCapturer.stopCapture();cameraCapturer.dispose();}catch(Exception ignored){}if(audioTrack!=null)audioTrack.setEnabled(false);if(factory!=null){factory.dispose();factory=null;}}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){super.onDestroy();stopMedia();}
}
