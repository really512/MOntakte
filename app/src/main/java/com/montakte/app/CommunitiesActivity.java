package com.montakte.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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

public class CommunitiesActivity extends Activity {
    private LinearLayout list;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        showScreen();
    }

    private TextView text(String s, float size) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(Color.BLACK);
        v.setPadding(8, 10, 8, 10);
        return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(16);
        return b;
    }

    private void showScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = text("Группы и каналы", 28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        Button create = button("＋ Создать группу или канал");
        root.addView(create);
        create.setOnClickListener(v -> startActivity(new Intent(this, GroupChannelActivity.class)));

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button back = button("← Назад");
        root.addView(back);
        back.setOnClickListener(v -> finish());
        setContentView(root);

        loadCommunities();
    }

    private void loadCommunities() {
        db.collection("communities")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { Toast.makeText(this, "Не удалось загрузить сообщества", Toast.LENGTH_SHORT).show(); return; }
                    list.removeAllViews();
                    if (snap == null || snap.isEmpty()) {
                        list.addView(text("Пока нет групп и каналов.", 18));
                        return;
                    }
                    for (DocumentSnapshot d : snap.getDocuments()) renderCommunity(d);
                });
    }

    private void renderCommunity(DocumentSnapshot d) {
        String type = d.getString("type");
        String name = d.getString("name");
        String description = d.getString("description");
        boolean isPublic = Boolean.TRUE.equals(d.getBoolean("isPublic"));
        String icon = "channel".equals(type) ? "📢" : "👥";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(8, 14, 8, 14);
        card.addView(text(icon + " " + (name == null ? "Без названия" : name), 20));
        if (description != null && !description.isEmpty()) card.addView(text(description, 16));
        card.addView(text(isPublic ? "🌐 Публичная" : "🔒 Приватная", 14));

        Button open = button("Открыть");
        card.addView(open);
        open.setOnClickListener(v -> Toast.makeText(this, "Экран сообщества будет следующим шагом", Toast.LENGTH_SHORT).show());
        list.addView(card);
    }

    @Override protected void onResume() {
        super.onResume();
        if (list != null) loadCommunities();
    }
}
