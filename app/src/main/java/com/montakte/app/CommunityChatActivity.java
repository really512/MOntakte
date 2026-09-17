package com.montakte.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class CommunityChatActivity extends Activity {
    private FirebaseFirestore db;
    private FirebaseUser user;
    private String communityId;
    private String type;
    private LinearLayout messages;
    private EditText input;
    private Button send;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        communityId = getIntent().getStringExtra("communityId");
        if (user == null || communityId == null) { finish(); return; }
        loadCommunity();
    }

    private TextView text(String s, float size) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(Color.BLACK);
        v.setPadding(10, 10, 10, 10); return v;
    }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(16); return b; }

    private void loadCommunity() {
        db.collection("communities").document(communityId).get().addOnSuccessListener(d -> {
            if (!d.exists()) { finish(); return; }
            type = d.getString("type");
            String name = d.getString("name");
            showChat(name == null ? "Сообщество" : name);
        }).addOnFailureListener(e -> { Toast.makeText(this, "Не удалось открыть сообщество", Toast.LENGTH_LONG).show(); finish(); });
    }

    private void showChat(String name) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,16);
        TextView title = text(("channel".equals(type) ? "📢 " : "👥 ") + name, 25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setGravity(Gravity.CENTER);
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));

        input = new EditText(this); input.setHint("Сообщение"); input.setSingleLine(false);
        send = button("Отправить");
        LinearLayout bar = new LinearLayout(this); bar.addView(input,new LinearLayout.LayoutParams(0,-2,1)); bar.addView(send);
        root.addView(bar);
        Button back = button("← Назад"); root.addView(back);
        back.setOnClickListener(v -> finish());
        send.setOnClickListener(v -> sendMessage());
        setContentView(root);
        loadMessages();
    }

    private void loadMessages() {
        db.collection("communities").document(communityId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(100)
                .addSnapshotListener((snap,e) -> {
                    if(e != null || snap == null) return;
                    messages.removeAllViews();
                    for(DocumentSnapshot d : snap.getDocuments()) {
                        String sender=d.getString("senderUid"), body=d.getString("text");
                        messages.addView(text("👤 " + (sender == null ? "Пользователь" : sender) + "\n" + (body == null ? "" : body),16));
                    }
                });
    }

    private void sendMessage() {
        String s=input.getText().toString().trim();
        if(s.isEmpty()) return;
        send.setEnabled(false);
        Map<String,Object> m=new HashMap<>();
        m.put("senderUid",user.getUid()); m.put("text",s); m.put("createdAt", FieldValue.serverTimestamp());
        db.collection("communities").document(communityId).collection("messages").add(m)
                .addOnSuccessListener(x -> { input.setText(""); send.setEnabled(true); })
                .addOnFailureListener(e -> { send.setEnabled(true); Toast.makeText(this,"Не удалось отправить: "+e.getMessage(),Toast.LENGTH_LONG).show(); });
    }
}
