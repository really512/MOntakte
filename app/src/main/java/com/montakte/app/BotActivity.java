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

import java.util.HashMap;
import java.util.Map;

public class BotActivity extends Activity {
    private FirebaseFirestore db; private FirebaseUser user; private LinearLayout list;
    @Override protected void onCreate(Bundle b){super.onCreate(b);db=FirebaseFirestore.getInstance();user=FirebaseAuth.getInstance().getCurrentUser();if(user==null){finish();return;}show();}
    private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.BLACK);v.setPadding(10,10,10,10);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(16);return b;}
    private void show(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,20,20,20);TextView title=text("🤖 Боты",28);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);Button create=button("＋ Создать бота");root.addView(create);create.setOnClickListener(v->create());ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));Button back=button("← Назад");root.addView(back);back.setOnClickListener(v->finish());setContentView(root);load();}
    private void load(){db.collection("bots").get().addOnSuccessListener(s->{list.removeAllViews();if(s.isEmpty()){list.addView(text("Ботов пока нет. Создай первого!",18));return;}for(DocumentSnapshot d:s.getDocuments()){String n=d.getString("name"),desc=d.getString("description");Button b=button("🤖 "+(n==null?"Без имени":n));list.addView(b);if(desc!=null&&!desc.isEmpty())list.addView(text(desc,15));b.setOnClickListener(v->{android.content.Intent i=new android.content.Intent(this,BotChatActivity.class);i.putExtra("botId",d.getId());startActivity(i);});}}).addOnFailureListener(e->Toast.makeText(this,"Не удалось загрузить ботов",Toast.LENGTH_SHORT).show());}
    private void create(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,20,20,20);TextView title=text("Создать бота",25);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title);EditText name=new EditText(this);name.setHint("Имя бота");root.addView(name);EditText desc=new EditText(this);desc.setHint("Описание");desc.setMinLines(3);root.addView(desc);Button save=button("Создать");root.addView(save);Button back=button("← Назад");root.addView(back);back.setOnClickListener(v->show());setContentView(root);save.setOnClickListener(v->{String n=name.getText().toString().trim(),d=desc.getText().toString().trim();if(n.isEmpty()){name.setError("Введите имя");return;}Map<String,Object> bot=new HashMap<>();bot.put("name",n);bot.put("description",d);bot.put("ownerUid",user.getUid());bot.put("createdAt",FieldValue.serverTimestamp());bot.put("enabled",true);db.collection("bots").add(bot).addOnSuccessListener(x->{Toast.makeText(this,"Бот создан 🤖",Toast.LENGTH_SHORT).show();show();}).addOnFailureListener(e->Toast.makeText(this,"Не удалось создать бота",Toast.LENGTH_SHORT).show());});}
}
