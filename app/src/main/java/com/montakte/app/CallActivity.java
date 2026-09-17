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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.VideoCapturer;
import org.webrtc.MediaConstraints;
import java.util.HashMap;
import java.util.Map;

public class CallActivity extends Activity {
    private static final int PERMISSIONS = 700;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String peerUid;
    private boolean videoCall;
    private PeerConnectionFactory factory;
    private AudioTrack audioTrack;
    private VideoTrack videoTrack;
    private CameraVideoCapturer cameraCapturer;
    private TextView status;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        peerUid = getIntent().getStringExtra("peerUid");
        videoCall = getIntent().getBooleanExtra("videoCall", false);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        buildUi();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            (videoCall && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            if (videoCall) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, PERMISSIONS);
            else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS);
        } else startCall();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(24,24,24,24);
        status = new TextView(this);
        status.setText(videoCall ? "📹 Видеозвонок" : "📞 Аудиозвонок");
        status.setTextSize(24);
        root.addView(status);
        TextView peer = new TextView(this);
        peer.setText(peerUid == null ? "Собеседник" : "Собеседник: " + peerUid);
        peer.setTextSize(16);
        root.addView(peer);
        Button mute = new Button(this);
        mute.setText("🎙 Выключить микрофон");
        root.addView(mute);
        mute.setOnClickListener(v -> {
            if (audioTrack != null) audioTrack.setEnabled(!audioTrack.enabled());
            mute.setText(audioTrack != null && audioTrack.enabled() ? "🎙 Выключить микрофон" : "🔇 Включить микрофон");
        });
        Button end = new Button(this);
        end.setText("❌ Завершить звонок");
        root.addView(end);
        end.setOnClickListener(v -> endCall());
        setContentView(root);
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PERMISSIONS) {
            boolean ok = results.length > 0;
            for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) ok = false;
            if (ok) startCall(); else { Toast.makeText(this, "Для звонка нужен доступ к микрофону" + (videoCall ? " и камере" : ""), Toast.LENGTH_LONG).show(); finish(); }
        }
    }

    private void startCall() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory();
        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        audioTrack = factory.createAudioTrack("audio0", audioSource);
        if (videoCall) {
            VideoSource source = factory.createVideoSource(false);
            cameraCapturer = createCameraCapturer();
            if (cameraCapturer != null) {
                try { cameraCapturer.initialize(null, getApplicationContext(), source.getCapturerObserver()); cameraCapturer.startCapture(640, 480, 30); } catch (Exception ignored) {}
                videoTrack = factory.createVideoTrack("video0", source);
            }
        }
        createCallRecord();
        status.setText(videoCall ? "📹 Звонок создаётся…" : "📞 Звонок создаётся…");
    }

    private CameraVideoCapturer createCameraCapturer() {
        Camera2Enumerator e = new Camera2Enumerator(this);
        for (String name : e.getDeviceNames()) if (e.isFrontFacing(name)) return e.createCapturer(name, null);
        for (String name : e.getDeviceNames()) return e.createCapturer(name, null);
        return null;
    }

    private void createCallRecord() {
        if (auth.getCurrentUser() == null || peerUid == null) return;
        String caller = auth.getCurrentUser().getUid();
        Map<String,Object> call = new HashMap<>();
        call.put("callerUid", caller);
        call.put("receiverUid", peerUid);
        call.put("type", videoCall ? "video" : "audio");
        call.put("status", "ringing");
        call.put("createdAt", FieldValue.serverTimestamp());
        db.collection("calls").add(call).addOnSuccessListener(ref -> status.setText(videoCall ? "📹 Ожидание ответа…" : "📞 Ожидание ответа…"));
    }

    private void endCall() {
        if (cameraCapturer != null) try { cameraCapturer.stopCapture(); cameraCapturer.dispose(); } catch (Exception ignored) {}
        if (audioTrack != null) audioTrack.setEnabled(false);
        if (peerUid != null && auth.getCurrentUser() != null) {
            db.collection("calls").whereEqualTo("callerUid", auth.getCurrentUser().getUid()).whereEqualTo("receiverUid", peerUid).whereEqualTo("status", "ringing").get().addOnSuccessListener(s -> { for (com.google.firebase.firestore.DocumentSnapshot d : s) db.collection("calls").document(d.getId()).update("status", "ended"); });
        }
        finish();
    }

    @Override protected void onDestroy() { super.onDestroy(); if (factory != null) factory.dispose(); }
}
