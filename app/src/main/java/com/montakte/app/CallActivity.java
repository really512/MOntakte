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
import com.google.firebase.firestore.ListenerRegistration;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallActivity extends Activity {
 private static final int PERMISSIONS=700;
 private FirebaseFirestore db; private FirebaseAuth auth; private String peerUid,callId,callerUid; private boolean videoCall,incoming,closing;
 private PeerConnectionFactory factory; private PeerConnection peer; private AudioSource audioSource; private AudioTrack audioTrack; private VideoSource videoSource; private VideoTrack videoTrack; private CameraVideoCapturer cameraCapturer; private SurfaceTextureHelper surfaceHelper; private EglBase eglBase;
 private TextView status; private Button answer,reject,end; private ListenerRegistration callListener,offerListener,answerListener,callerIceListener,calleeIceListener;
 @Override protected void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();peerUid=getIntent().getStringExtra("peerUid");callId=getIntent().getStringExtra("callId");incoming=getIntent().getBooleanExtra("incoming",false);videoCall=getIntent().getBooleanExtra("videoCall",false);buildUi();if(auth.getCurrentUser()==null){finish();return;}if(callId!=null)loadCall();else requestPermissionsAndStart();}
 private void buildUi(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setPadding(28,28,28,28);status=new TextView(this);status.setTextSize(24);status.setGravity(Gravity.CENTER);status.setText(incoming?(videoCall?"📹 Входящий видеозвонок":"📞 Входящий звонок"):(videoCall?"📹 Видеозвонок":"📞 Звонок"));r.addView(status);TextView p=new TextView(this);p.setTextSize(16);p.setGravity(Gravity.CENTER);p.setText(peerUid==null?"Собеседник":"Собеседник: "+peerUid);r.addView(p);answer=new Button(this);answer.setText(videoCall?"📹 Принять видеозвонок":"📞 Принять звонок");reject=new Button(this);reject.setText("✖ Отклонить");end=new Button(this);end.setText("❌ Завершить");if(incoming){r.addView(answer);r.addView(reject);}r.addView(end);answer.setOnClickListener(v->acceptCall());reject.setOnClickListener(v->finishCall("rejected"));end.setOnClickListener(v->finishCall("ended"));setContentView(r);}
 private void loadCall(){db.collection("calls").document(callId).get().addOnSuccessListener(d->{if(!d.exists()){toast("Звонок уже недоступен");finish();return;}callerUid=d.getString("callerUid");String receiver=d.getString("receiverUid"),type=d.getString("type"),state=d.getString("status"),me=auth.getCurrentUser().getUid();if(!me.equals(callerUid)&&!me.equals(receiver)){finish();return;}peerUid=me.equals(callerUid)?receiver:callerUid;videoCall="video".equals(type);if("ringing".equals(state)&&incoming){listenCall();return;}if("ringing".equals(state)&&!incoming)requestPermissionsAndStart();else if("accepted".equals(state)){incoming=false;requestPermissionsAndStart();}else{toast("Звонок завершён");finish();}}).addOnFailureListener(e->{toast("Не удалось открыть звонок");finish();});}
 private void acceptCall(){db.collection("calls").document(callId).update("status","accepted","answeredAt",FieldValue.serverTimestamp()).addOnSuccessListener(v->{incoming=false;answer.setVisibility(android.view.View.GONE);reject.setVisibility(android.view.View.GONE);requestPermissionsAndStart();}).addOnFailureListener(e->toast("Не удалось принять звонок: "+e.getMessage()));}
 private void requestPermissionsAndStart(){boolean a=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED,c=!videoCall||checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;if(!a||!c)ActivityCompat.requestPermissions(this,videoCall?new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA}:new String[]{Manifest.permission.RECORD_AUDIO},PERMISSIONS);else startWebRtc();}
 @Override public void onRequestPermissionsResult(int rc,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(rc,p,g);if(rc==PERMISSIONS){boolean ok=g.length>0;for(int x:g)if(x!=PackageManager.PERMISSION_GRANTED)ok=false;if(ok)startWebRtc();else{toast("Нужен доступ к микрофону"+(videoCall?" и камере":""));finish();}}}
 private void startWebRtc(){try{PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());factory=PeerConnectionFactory.builder().createPeerConnectionFactory();audioSource=factory.createAudioSource(new MediaConstraints());audioTrack=factory.createAudioTrack("audio0",audioSource);if(videoCall)setupCamera();createPeer();listenCall();listenIce();if(callId==null)createCallRecord();else if(incoming)waitForOffer();else createOffer();status.setText(videoCall?"📹 Подключение…":"📞 Подключение…");}catch(Exception e){toast("Ошибка WebRTC: "+e.getMessage());finish();}}
 private void setupCamera(){videoSource=factory.createVideoSource(false);cameraCapturer=createCameraCapturer();if(cameraCapturer==null)throw new IllegalStateException("Камера не найдена");eglBase=EglBase.create();surfaceHelper=SurfaceTextureHelper.create("CallCamera",eglBase.getEglBaseContext());cameraCapturer.initialize(surfaceHelper,getApplicationContext(),videoSource.getCapturerObserver());cameraCapturer.startCapture(640,480,24);videoTrack=factory.createVideoTrack("video0",videoSource);}
 private CameraVideoCapturer createCameraCapturer(){Camera2Enumerator e=new Camera2Enumerator(this);for(String n:e.getDeviceNames())if(e.isFrontFacing(n)){CameraVideoCapturer c=e.createCapturer(n,null);if(c!=null)return c;}for(String n:e.getDeviceNames()){CameraVideoCapturer c=e.createCapturer(n,null);if(c!=null)return c;}return null;}
 private void createPeer(){List<PeerConnection.IceServer> servers=new ArrayList<>();servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());PeerConnection.RTCConfiguration cfg=new PeerConnection.RTCConfiguration(servers);peer=factory.createPeerConnection(cfg,new PeerConnection.Observer(){public void onSignalingChange(PeerConnection.SignalingState s){}public void onIceConnectionChange(PeerConnection.IceConnectionState s){}public void onIceConnectionReceivingChange(boolean b){}public void onIceGatheringChange(PeerConnection.IceGatheringState s){}public void onIceCandidate(IceCandidate c){writeCandidate(c);}public void onIceCandidatesRemoved(IceCandidate[] c){}public void onAddStream(MediaStream s){}public void onRemoveStream(MediaStream s){}public void onDataChannel(org.webrtc.DataChannel d){}public void onRenegotiationNeeded(){}});if(peer==null)throw new IllegalStateException("PeerConnection unavailable");MediaStream stream=factory.createLocalMediaStream("media");stream.addTrack(audioTrack);if(videoCall&&videoTrack!=null)stream.addTrack(videoTrack);peer.addStream(stream);}
 private void createCallRecord(){if(peerUid==null)return;callerUid=auth.getCurrentUser().getUid();Map<String,Object> c=new HashMap<>();c.put("callerUid",callerUid);c.put("receiverUid",peerUid);c.put("type",videoCall?"video":"audio");c.put("status","ringing");c.put("createdAt",FieldValue.serverTimestamp());db.collection("calls").add(c).addOnSuccessListener(d->{callId=d.getId();listenCall();listenIce();status.setText(videoCall?"📹 Ожидание ответа…":"📞 Ожидание ответа…");});}
 private void createOffer(){if(peer==null)return;peer.createOffer(new SdpObserver(){public void onCreateSuccess(SessionDescription s){peer.setLocalDescription(new EmptySdp(),s);putSignal("offer",s);}public void onSetSuccess(){}public void onCreateFailure(String s){toast("Offer: "+s);}public void onSetFailure(String s){}},new MediaConstraints());}
 private void waitForOffer(){if(offerListener!=null)return;offerListener=db.collection("calls").document(callId).addSnapshotListener((d,e)->{if(e!=null||d==null||!d.exists()||peer==null)return;Map<String,Object> o=(Map<String,Object>)d.get("offer");if(o!=null&&peer.getRemoteDescription()==null)peer.setRemoteDescription(new SdpObserver(){public void onSetSuccess(){createAnswer();}public void onSetFailure(String s){toast("Offer: "+s);}public void onCreateSuccess(SessionDescription s){}public void onCreateFailure(String s){}},new SessionDescription(SessionDescription.Type.OFFER,String.valueOf(o.get("sdp"))));});}
 private void createAnswer(){peer.createAnswer(new SdpObserver(){public void onCreateSuccess(SessionDescription s){peer.setLocalDescription(new EmptySdp(),s);putSignal("answer",s);}public void onSetSuccess(){}public void onCreateFailure(String s){toast("Answer: "+s);}public void onSetFailure(String s){}},new MediaConstraints());}
 private void putSignal(String key,SessionDescription s){if(callId==null)return;Map<String,Object>x=new HashMap<>();x.put("type",s.type.canonicalForm());x.put("sdp",s.description);db.collection("calls").document(callId).update(key,x);if("offer".equals(key))listenForAnswer();}
 private void listenForAnswer(){if(answerListener!=null)return;answerListener=db.collection("calls").document(callId).addSnapshotListener((d,e)->{if(e!=null||d==null||!d.exists()||peer==null)return;Map<String,Object>a=(Map<String,Object>)d.get("answer");if(a!=null&&peer.getRemoteDescription()==null)peer.setRemoteDescription(new EmptySdp(),new SessionDescription(SessionDescription.Type.ANSWER,String.valueOf(a.get("sdp"))));});}
 private boolean caller(){return auth.getCurrentUser().getUid().equals(callerUid);}
 private void writeCandidate(IceCandidate c){if(callId==null)return;Map<String,Object>x=new HashMap<>();x.put("sdpMid",c.sdpMid);x.put("sdpMLineIndex",c.sdpMLineIndex);x.put("candidate",c.sdp);db.collection("calls").document(callId).collection(caller()?"callerCandidates":"calleeCandidates").add(x);}
 private void listenIce(){if(callId==null)return;if(callerIceListener==null)callerIceListener=db.collection("calls").document(callId).collection("callerCandidates").addSnapshotListener((s,e)->{if(e==null&&s!=null)for(DocumentSnapshot d:s.getDocuments())if(!caller())addCandidate(d);});if(calleeIceListener==null)calleeIceListener=db.collection("calls").document(callId).collection("calleeCandidates").addSnapshotListener((s,e)->{if(e==null&&s!=null)for(DocumentSnapshot d:s.getDocuments())if(caller())addCandidate(d);});}
 private void addCandidate(DocumentSnapshot d){if(peer==null||!d.exists())return;Long i=d.getLong("sdpMLineIndex");String mid=d.getString("sdpMid"),c=d.getString("candidate");if(i!=null&&c!=null)peer.addIceCandidate(new IceCandidate(mid,i.intValue(),c));}
 private void listenCall(){if(callId==null||callListener!=null)return;callListener=db.collection("calls").document(callId).addSnapshotListener((d,e)->{if(e!=null||d==null||!d.exists())return;String s=d.getString("status");if("accepted".equals(s)){if(!incoming&&peer!=null&&peer.getLocalDescription()==null)createOffer();if(incoming&&peer!=null)waitForOffer();}else if("rejected".equals(s)||"ended".equals(s)){toast("Звонок завершён");finish();}});}
 private void finishCall(String state){if(!closing&&callId!=null)db.collection("calls").document(callId).update("status",state,"endedAt",FieldValue.serverTimestamp());closing=true;stopWebRtc();finish();}
 private void stopWebRtc(){if(callListener!=null)callListener.remove();if(offerListener!=null)offerListener.remove();if(answerListener!=null)answerListener.remove();if(callerIceListener!=null)callerIceListener.remove();if(calleeIceListener!=null)calleeIceListener.remove();if(cameraCapturer!=null)try{cameraCapturer.stopCapture();cameraCapturer.dispose();}catch(Exception ignored){}if(surfaceHelper!=null)surfaceHelper.dispose();if(peer!=null){peer.close();peer.dispose();peer=null;}if(audioSource!=null)audioSource.dispose();if(videoSource!=null)videoSource.dispose();if(factory!=null){factory.dispose();factory=null;}if(eglBase!=null){eglBase.release();eglBase=null;}}
 private static class EmptySdp implements SdpObserver{public void onSetSuccess(){}public void onSetFailure(String s){}public void onCreateSuccess(SessionDescription s){}public void onCreateFailure(String s){}}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){closing=true;stopWebRtc();super.onDestroy();}
}
