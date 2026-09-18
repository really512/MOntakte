package com.montakte.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.*;
import androidx.annotation.NonNull;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private LinearLayout root, content;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private static final int PICK_MEDIA = 901;
    private Uri selectedMedia;
    private final String accent = "#2684FF";
    private final int ink = Color.parseColor("#14213D");
    private final int muted = Color.parseColor("#667085");
    private final int page = Color.parseColor("#F5F7FB");

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
            FirebaseUser u = auth.getCurrentUser();
            if (u == null) openAuth(); else checkProfile(u);
        } catch (Exception e) { openAuth(); }
    }

    private GradientDrawable bg(String c, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(c));
        g.setCornerRadius(r);
        return g;
    }

    private TextView text(String s, float z, int c) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(z); v.setTextColor(c);
        v.setPadding(0, 4, 0, 4);
        return v;
    }

    private TextView center(String s, float z, int c) {
        TextView v = text(s, z, c); v.setGravity(Gravity.CENTER); return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackground(bg(accent, 28));
        b.setPadding(18, 3, 18, 3);
        return b;
    }

    private Button softButton(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(Color.parseColor(accent));
        b.setBackground(bg("#EAF3FF", 24));
        b.setPadding(14, 3, 14, 3);
        return b;
    }

    private EditText input(String h) {
        EditText e = new EditText(this);
        e.setHint(h); e.setTextSize(16); e.setSingleLine();
        e.setPadding(18, 0, 18, 0); e.setBackground(bg("#FFFFFF", 22));
        return e;
    }

    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(page);
        setContentView(root);
    }

    private void authBase() {
        base(); root.setGravity(Gravity.CENTER); root.setPadding(28, 24, 28, 24);
    }

    private void logo() {
        TextView l = center("ВО", 42, Color.WHITE);
        l.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        l.setBackground(bg(accent, 32));
        root.addView(l, new LinearLayout.LayoutParams(104, 104));
    }

    private void header(String title, String sub) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(16, 18, 16, 8);
        TextView x = text(title, 28, ink);
        x.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        h.addView(x);
        if (sub != null) h.addView(text(sub, 14, muted));
        root.addView(h);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(18, 16, 18, 16);
        c.setBackground(bg("#FFFFFF", 22));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(10, 4, 10, 10);
        c.setLayoutParams(p);
        return c;
    }

    private void addBack(Button b, FirebaseUser u) {
        b.setText("← В ленту"); b.setAllCaps(false);
        b.setTextColor(Color.parseColor("#344054"));
        b.setBackground(bg("#E2E4E8", 4));
        root.addView(b, new LinearLayout.LayoutParams(-1, 52));
        b.setOnClickListener(v -> feed(u));
    }

    private void openAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    private void checkProfile(FirebaseUser u) {
        db.collection("users").document(u.getUid()).get().addOnSuccessListener(d -> {
            String n = d.getString("displayName");
            if (n == null || n.trim().isEmpty()) profileSetup(u); else feed(u);
        }).addOnFailureListener(e -> profileSetup(u));
    }

    private void profileSetup(FirebaseUser u) {
        authBase(); logo();
        Space s = new Space(this); root.addView(s, new LinearLayout.LayoutParams(1, 16));
        root.addView(center("Создайте профиль", 25, ink));
        root.addView(center("Остался один шаг — ваше имя", 15, muted));
        Space s2 = new Space(this); root.addView(s2, new LinearLayout.LayoutParams(1, 20));
        EditText name = input("Имя и фамилия"); root.addView(name, new LinearLayout.LayoutParams(-1, 54));
        Space s3 = new Space(this); root.addView(s3, new LinearLayout.LayoutParams(1, 14));
        EditText avatar = input("Аватар, например 🦊 (необязательно)"); root.addView(avatar, new LinearLayout.LayoutParams(-1, 54));
        Space s4 = new Space(this); root.addView(s4, new LinearLayout.LayoutParams(1, 14));
        Button save = button("Создать профиль"); root.addView(save, new LinearLayout.LayoutParams(-1, 52));
        save.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.length() < 2) { name.setError("Введите имя"); return; }
            Map<String,Object> m = new HashMap<>();
            m.put("uid", u.getUid()); m.put("email", u.getEmail());
            m.put("displayName", n); m.put("avatar", avatar.getText().toString().trim());
            m.put("createdAt", FieldValue.serverTimestamp()); m.put("updatedAt", FieldValue.serverTimestamp());
            save.setEnabled(false);
            db.collection("users").document(u.getUid()).set(m, SetOptions.merge())
                .addOnSuccessListener(x -> feed(u))
                .addOnFailureListener(e -> { save.setEnabled(true); toast("Не удалось создать профиль: " + e.getMessage()); });
        });
    }

    private void feed(FirebaseUser u) {
        base();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(16, 8, 16, 6);
        TextView brand = text("ВОнтакте", 28, ink);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(brand, new LinearLayout.LayoutParams(0, 58, 1));
        Button prof = button("Проф");
        top.addView(prof, new LinearLayout.LayoutParams(92, 48));
        root.addView(top);

        ScrollView sc = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, 88); sc.addView(content);
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));

        composer(u); loadPosts(u); bottomNav(u, 0);
        prof.setOnClickListener(v -> profile(u));
    }

    private void composer(FirebaseUser u) {
        LinearLayout c = card();
        c.addView(text("Что нового?", 18, muted));
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        Button post = button("＋ Создать пост");
        Button media = softButton("Фото/видео");
        r.addView(post);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-2, 48); mp.setMargins(6,0,0,0);
        r.addView(media, mp); c.addView(r); content.addView(c);
        post.setOnClickListener(v -> createPost(u)); media.setOnClickListener(v -> createPost(u));
    }

    private void loadPosts(FirebaseUser u) {
        TextView h = text("Лента", 22, ink); h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        h.setPadding(14, 8, 14, 8); content.addView(h);
        db.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                while (content.getChildCount() > 2) content.removeViewAt(2);
                if (snap.isEmpty()) {
                    LinearLayout c = card();
                    c.addView(center("Постов пока нет", 19, ink));
                    c.addView(center("Создайте первый пост — он появится здесь.", 14, muted));
                    content.addView(c); return;
                }
                for (DocumentSnapshot d : snap.getDocuments()) renderPost(d, u);
            });
    }

    private void renderPost(DocumentSnapshot d, FirebaseUser u) {
        String author = d.getString("authorUid"), body = d.getString("text");
        String url = d.getString("mediaUrl"), type = d.getString("mediaType");
        Long likes = d.getLong("likeCount");
        LinearLayout c = card();
        TextView who = text("Загрузка профиля…", 15, Color.parseColor("#536174"));
        who.setTypeface(Typeface.DEFAULT, Typeface.BOLD); c.addView(who);
        if (author != null) db.collection("users").document(author).get().addOnSuccessListener(x -> {
            String n = x.getString("displayName"), a = x.getString("avatar");
            who.setText((a == null || a.isEmpty() ? "👤" : a) + "  " + (n == null || n.isEmpty() ? "Пользователь" : n));
        });
        if (body != null && !body.isEmpty()) {
            TextView b = text(body, 17, ink); b.setPadding(0, 12, 0, 12); c.addView(b);
        }
        if (url != null && !url.isEmpty()) {
            if (type != null && type.startsWith("video/")) {
                VideoView v = new VideoView(this); v.setVideoURI(Uri.parse(url));
                c.addView(v, new LinearLayout.LayoutParams(-1, 420));
                Button play = button("▶ Смотреть видео"); c.addView(play); play.setOnClickListener(x -> v.start());
            } else {
                ImageView iv = new ImageView(this); iv.setAdjustViewBounds(true);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                c.addView(iv, new LinearLayout.LayoutParams(-1, 420));
                com.bumptech.glide.Glide.with(this).load(url).into(iv);
            }
        }
        LinearLayout actions = new LinearLayout(this);
        Button like = new Button(this), comments = new Button(this);
        like.setAllCaps(false); comments.setAllCaps(false);
        like.setTextColor(Color.parseColor("#536174")); comments.setTextColor(Color.parseColor("#536174"));
        like.setBackgroundColor(Color.TRANSPARENT); comments.setBackgroundColor(Color.TRANSPARENT);
        like.setText("♡ " + (likes == null ? 0 : likes)); comments.setText("💬 Комментарии");
        actions.addView(like, new LinearLayout.LayoutParams(0, 52, 1));
        actions.addView(comments, new LinearLayout.LayoutParams(0, 52, 1)); c.addView(actions);
        content.addView(c);
        String pid = d.getId();
        db.collection("posts").document(pid).collection("likes").document(u.getUid()).get()
            .addOnSuccessListener(x -> like.setText((x.exists() ? "♥" : "♡") + " " + (likes == null ? 0 : likes)));
        like.setOnClickListener(v -> toggleLike(pid, u));
        comments.setOnClickListener(v -> comments(u, pid));
    }

    private void toggleLike(String pid, FirebaseUser u) {
        DocumentReference p = db.collection("posts").document(pid);
        DocumentReference l = p.collection("likes").document(u.getUid());
        l.get().addOnSuccessListener(d -> {
            if (d.exists()) l.delete().addOnSuccessListener(x -> p.update("likeCount", FieldValue.increment(-1)));
            else {
                Map<String,Object> m = new HashMap<>(); m.put("uid", u.getUid());
                l.set(m).addOnSuccessListener(x -> p.update("likeCount", FieldValue.increment(1)));
            }
        });
    }

    private void createPost(FirebaseUser u) {
        base(); header("Новый пост", "Поделитесь с друзьями");
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(18,8,18,18);
        EditText body = input("Что хотите рассказать?"); body.setGravity(Gravity.TOP); body.setMinLines(5);
        box.addView(body, new LinearLayout.LayoutParams(-1,150));
        TextView selected = text("Медиа не выбрано",14,muted); box.addView(selected);
        Button media = softButton("📎 Добавить фото или видео"); box.addView(media,new LinearLayout.LayoutParams(-1,50));
        Button publish = button("Опубликовать"); LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,52); pp.setMargins(0,12,0,0); box.addView(publish,pp);
        Button back = softButton("Назад"); box.addView(back);
        root.addView(box); selectedMedia=null;
        media.setOnClickListener(v -> { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"}); startActivityForResult(i,PICK_MEDIA); });
        back.setOnClickListener(v -> feed(u));
        publish.setOnClickListener(v -> { String s=body.getText().toString().trim(); if(s.isEmpty()&&selectedMedia==null){body.setError("Добавьте текст или фото/видео");return;} publish.setEnabled(false); if(selectedMedia==null) savePost(u,s,null,null,publish); else uploadPost(u,s,publish); });
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_MEDIA&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            selectedMedia=data.getData();
            if(root!=null) for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);if(v instanceof TextView&&((TextView)v).getText().toString().equals("Медиа не выбрано")){((TextView)v).setText("✓ Медиа выбрано");break;}}
        }
    }

    private void uploadPost(FirebaseUser u,String body,Button publish) {
        String mime=getContentResolver().getType(selectedMedia);
        if(mime==null||(!mime.startsWith("image/")&&!mime.startsWith("video/"))){publish.setEnabled(true);toast("Выберите фото или видео");return;}
        String ext=MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);if(ext==null)ext="bin";
        StorageReference r=storage.getReference().child("post_media").child(u.getUid()).child(UUID.randomUUID()+"."+ext);
        toast("Загружаем медиа…");
        r.putFile(selectedMedia).continueWithTask(t->{if(!t.isSuccessful()&&t.getException()!=null)throw t.getException();return r.getDownloadUrl();})
            .addOnSuccessListener(x->savePost(u,body,x.toString(),mime,publish))
            .addOnFailureListener(e->{publish.setEnabled(true);toast("Ошибка загрузки: "+e.getMessage());});
    }

    private void savePost(FirebaseUser u,String body,String url,String mime,Button publish) {
        Map<String,Object> p=new HashMap<>();p.put("authorUid",u.getUid());p.put("text",body);p.put("likeCount",0);p.put("createdAt",FieldValue.serverTimestamp());
        if(url!=null){p.put("mediaUrl",url);p.put("mediaType",mime);}
        db.collection("posts").add(p).addOnSuccessListener(x->{toast("Пост опубликован ✓");feed(u);})
            .addOnFailureListener(e->{publish.setEnabled(true);toast("Ошибка публикации: "+e.getMessage());});
    }

    private void comments(FirebaseUser u,String pid) {
        base(); header("Комментарии","Обсуждение поста");
        ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(4,0,4,10);sc.addView(list);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bar=new LinearLayout(this);EditText in=input("Написать комментарий…");Button send=button("Отправить");
        bar.addView(in,new LinearLayout.LayoutParams(0,54,1));bar.addView(send,new LinearLayout.LayoutParams(120,54));root.addView(bar);
        Button back=softButton("Назад");root.addView(back);back.setOnClickListener(v->feed(u));
        db.collection("posts").document(pid).collection("comments").orderBy("createdAt",Query.Direction.ASCENDING).addSnapshotListener((snap,e)->{
            if(e!=null||snap==null)return;list.removeAllViews();for(DocumentSnapshot d:snap.getDocuments()){LinearLayout c=card();c.addView(text(d.getString("text"),15,ink));list.addView(c);}
        });
        send.setOnClickListener(v->{String s=in.getText().toString().trim();if(s.isEmpty())return;Map<String,Object> c=new HashMap<>();c.put("authorUid",u.getUid());c.put("text",s);c.put("createdAt",FieldValue.serverTimestamp());db.collection("posts").document(pid).collection("comments").add(c).addOnSuccessListener(x->in.setText(""));});
    }

    private void profile(FirebaseUser u) {
        base(); header("Профиль","Ваш аккаунт ВОнтакте");
        LinearLayout c=card(); LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView av=center("👤",38,Color.WHITE);av.setBackground(bg(accent,50));row.addView(av,new LinearLayout.LayoutParams(76,76));
        Space sp=new Space(this);row.addView(sp,new LinearLayout.LayoutParams(18,1));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        TextView name=center("Загрузка…",22,ink);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);info.addView(name);
        TextView email=center("",14,muted);info.addView(email);row.addView(info,new LinearLayout.LayoutParams(0,80,1));c.addView(row);root.addView(c);
        db.collection("users").document(u.getUid()).get().addOnSuccessListener(d->{name.setText(d.getString("displayName")==null?"Пользователь":d.getString("displayName"));email.setText(d.getString("email")==null?"":d.getString("email"));});
        Button out=button("Выйти");root.addView(out,new LinearLayout.LayoutParams(-1,52));
        Button back=softButton("← В ленту");root.addView(back);back.setOnClickListener(v->feed(u));
        out.setOnClickListener(v->{auth.signOut();openAuth();});
    }

    private void friends(FirebaseUser u) {
        base();header("Друзья","Ваши друзья");ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        db.collection("friendships").whereEqualTo("userA",u.getUid()).get().addOnSuccessListener(s->friendRows(s.getDocuments(),u,list));
        db.collection("friendships").whereEqualTo("userB",u.getUid()).get().addOnSuccessListener(s->friendRows(s.getDocuments(),u,list));
        Button back=softButton("← В ленту");root.addView(back);back.setOnClickListener(v->feed(u));
    }

    private void friendRows(List<DocumentSnapshot> ds,FirebaseUser u,LinearLayout list) {
        for(DocumentSnapshot d:ds){String a=d.getString("userA"),b=d.getString("userB"),o=u.getUid().equals(a)?b:a;if(o!=null)db.collection("users").document(o).get().addOnSuccessListener(x->{LinearLayout c=card();c.addView(text("👤 "+(x.getString("displayName")==null?"Пользователь":x.getString("displayName")),17,ink));list.addView(c);});}
    }

    private void messages(FirebaseUser u) {
        base();
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(16,18,16,8);
        TextView t=text("Сообщения",28,ink);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.addView(t,new LinearLayout.LayoutParams(0,54,1));
        Button add=softButton("+");h.addView(add,new LinearLayout.LayoutParams(54,48));root.addView(h);
        EditText search=input("⌕  Поиск");root.addView(search,new LinearLayout.LayoutParams(-1,54));
        ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        String[] names={"Данил","Аня","Макс","Катя","Группа ВОнтакте","Илья"};
        String[] msgs={"Привет! Как дела?","Хорошо, спасибо!","Скинь фотку","Ок, давай завтра","Привет всем!","Увидимся завтра"};
        for(int i=0;i<names.length;i++){LinearLayout c=card();LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView av=center(i%2==0?"👤":"●",28,Color.WHITE);av.setBackground(bg(accent,50));r.addView(av,new LinearLayout.LayoutParams(52,52));Space sp=new Space(this);r.addView(sp,new LinearLayout.LayoutParams(12,1));LinearLayout inf=new LinearLayout(this);inf.setOrientation(LinearLayout.VERTICAL);inf.addView(text(names[i],16,ink));inf.addView(text(msgs[i],14,muted));r.addView(inf,new LinearLayout.LayoutParams(0,60,1));r.addView(text(i<4?String.valueOf(i+1):"Вчера",12,muted));c.addView(r);list.addView(c);}
        bottomNav(u,1);
    }

    private void notifications(FirebaseUser u) {
        base();header("Уведомления","Что произошло в ВОнтакте");
        String[] titles={"Новые лайки","Комментарии","Новый друг","Группа ВОнтакте","Обновление"};
        String[] subs={"Кто-то оценил вашу запись","К вашей записи добавили комментарий","Екатерина хочет добавить вас в друзья","Новое сообщение в группе","Доступно новое обновление приложения"};
        String[] icons={"♥","●","👤","👥","ℹ"};
        ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<titles.length;i++){LinearLayout c=card();LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView ic=center(icons[i],24,Color.WHITE);ic.setBackground(bg(i%2==0?"#FF3B5C":accent,50));r.addView(ic,new LinearLayout.LayoutParams(52,52));Space sp=new Space(this);r.addView(sp,new LinearLayout.LayoutParams(12,1));LinearLayout inf=new LinearLayout(this);inf.setOrientation(LinearLayout.VERTICAL);inf.addView(text(titles[i],16,ink));inf.addView(text(subs[i],14,muted));r.addView(inf,new LinearLayout.LayoutParams(0,72,1));c.addView(r);list.addView(c);}
        bottomNav(u,2);
    }

    private void settings(FirebaseUser u) {
        base();header("Настройки",null);
        LinearLayout account=card();account.addView(text("Аккаунт",14,muted));account.addView(text("👤  Личные данные                                      ›",16,ink));account.addView(text("☎  Номер телефона",16,ink));account.addView(text("✉  Почта",16,ink));root.addView(account);
        LinearLayout privacy=card();privacy.addView(text("Приватность",14,muted));privacy.addView(text("Кто может видеть мою страницу                         ›",15,ink));privacy.addView(text("Кто может писать мне                                    ›",15,ink));privacy.addView(text("Кто может приглашать в друзья                         ›",15,ink));privacy.addView(text("Блокировка пользователей                              ›",15,ink));root.addView(privacy);
        LinearLayout appearance=card();appearance.addView(text("Уведомления и внешний вид",14,muted));appearance.addView(text("🔔 Push-уведомления                              ВКЛ",15,ink));appearance.addView(text("🔊 Звук уведомлений                                  ВКЛ",15,ink));appearance.addView(text("◐ Тёмная тема                                      ВЫКЛ",15,ink));root.addView(appearance);
        Button out=button("Выйти из аккаунта");out.setTextColor(Color.parseColor("#E5484D"));out.setBackground(bg("#FFFFFF",22));root.addView(out,new LinearLayout.LayoutParams(-1,52));out.setOnClickListener(v->{auth.signOut();openAuth();});
        Button back=softButton("← В ленту");root.addView(back);back.setOnClickListener(v->feed(u));
    }

    private void menu(FirebaseUser u) {
        base();header("Меню","ВОнтакте");
        Button groups=button("👥 Группы");Button channels=button("📢 Каналы");Button bots=button("🤖 Боты");Button settings=softButton("⚙ Настройки");
        root.addView(groups,new LinearLayout.LayoutParams(-1,52));root.addView(channels,new LinearLayout.LayoutParams(-1,52));root.addView(bots,new LinearLayout.LayoutParams(-1,52));root.addView(settings,new LinearLayout.LayoutParams(-1,52));
        Button back=softButton("← В ленту");root.addView(back);
        groups.setOnClickListener(v->startActivity(new Intent(this,CommunitiesActivity.class).putExtra("communityType","group")));
        channels.setOnClickListener(v->startActivity(new Intent(this,CommunitiesActivity.class).putExtra("communityType","channel")));
        bots.setOnClickListener(v->startActivity(new Intent(this,BotActivity.class)));
        settings.setOnClickListener(v->settings(u));back.setOnClickListener(v->feed(u));
    }

    private void bottomNav(FirebaseUser u, int selected) {
        LinearLayout nav=new LinearLayout(this);nav.setPadding(8,4,8,6);nav.setBackgroundColor(Color.WHITE);
        String[] labels={"⌂\nГлавная","✉\nСообщения","♧\nУведомления","♙\nПрофиль"};
        for(int i=0;i<labels.length;i++){
            Button b=new Button(this);b.setText(labels[i]);b.setTextSize(12);b.setAllCaps(false);
            b.setTextColor(Color.parseColor(i==selected?accent:"#536174"));b.setBackgroundColor(Color.TRANSPARENT);
            nav.addView(b,new LinearLayout.LayoutParams(0,64,1));
            final int n=i;
            if(i==0)b.setOnClickListener(v->feed(u));
            if(i==1)b.setOnClickListener(v->messages(u));
            if(i==2)b.setOnClickListener(v->notifications(u));
            if(i==3)b.setOnClickListener(v->profile(u));
        }
        root.addView(nav);
    }
}
