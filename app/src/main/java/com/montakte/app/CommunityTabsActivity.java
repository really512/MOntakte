package com.montakte.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class CommunityTabsActivity extends Activity {
    private LinearLayout root;
    private LinearLayout content;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { Toast.makeText(this, "Сначала войдите в ВОнтакте", Toast.LENGTH_LONG).show(); finish(); return; }
        show("messages");
    }

    private TextView text(String s, float size) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(Color.BLACK);
        v.setPadding(8, 10, 8, 10); return v;
    }

    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(15); return b; }

    private void base() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,16);
        TextView title = text("ВОнтакте", 28); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setGravity(Gravity.CENTER); root.addView(title);
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button messages = button("💬 Сообщения"), groups = button("👥 Группы"), channels = button("📢 Каналы");
        tabs.addView(messages, new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(groups, new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(channels, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(tabs);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        Button back = button("← Назад"); root.addView(back); back.setOnClickListener(v -> finish());
        messages.setOnClickListener(v -> show("messages"));
        groups.setOnClickListener(v -> show("group"));
        channels.setOnClickListener(v -> show("channel"));
        setContentView(root);
    }

    private void show(String tab) {
        if (root == null) base();
        content.removeAllViews();
        if ("messages".equals(tab)) loadMessages(); else loadCommunities(tab);
    }

    private void loadMessages() {
        content.addView(text("Сообщения", 23));
        Button newChat = button("＋ Новый чат"); content.addView(newChat);
        db.collection("conversations").whereArrayContains("participants", user.getUid()).get().addOnSuccessListener(s -> {
            if (s.isEmpty()) content.addView(text("Диалогов пока нет",18));
            for (DocumentSnapshot d : s.getDocuments()) {
                List<String> ps = (List<String>) d.get("participants");
                if (ps == null) continue;
                for (String uid : ps) if (!user.getUid().equals(uid)) {
                    db.collection("users").document(uid).get().addOnSuccessListener(x -> {
                        String n = x.getString("displayName");
                        Button b = button("💬 " + (n == null || n.isEmpty() ? x.getString("phone") : n));
                        content.addView(b);
                        b.setOnClickListener(v -> { Toast.makeText(this, "Чат откроется в разделе сообщений", Toast.LENGTH_SHORT).show(); });
                    });
                    break;
                }
            }
        });
    }

    private void loadCommunities(String type) {
        String title = "group".equals(type) ? "Группы" : "Каналы";
        content.addView(text(title, 23));
        Button create = button("＋ Создать " + ("group".equals(type) ? "группу" : "канал"));
        content.addView(create);
        create.setOnClickListener(v -> startActivity(new android.content.Intent(this, GroupChannelActivity.class)));
        db.collection("communities").whereEqualTo("type", type).orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().addOnSuccessListener(s -> {
            boolean any = false;
            for (DocumentSnapshot d : s.getDocuments()) {
                Boolean pub = d.getBoolean("isPublic");
                List<String> members = (List<String>) d.get("members");
                if (Boolean.TRUE.equals(pub) || (members != null && members.contains(user.getUid()))) {
                    any = true;
                    String name = d.getString("name");
                    String desc = d.getString("description");
                    Button b = button(("group".equals(type) ? "👥 " : "📢 ") + (name == null ? "Без названия" : name));
                    content.addView(b);
                    if (desc != null && !desc.isEmpty()) content.addView(text(desc, 15));
                }
            }
            if (!any) content.addView(text("Пока ничего нет",18));
        }).addOnFailureListener(e -> content.addView(text("Не удалось загрузить список",17)));
    }
}
