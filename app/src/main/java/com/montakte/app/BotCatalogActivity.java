package com.montakte.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Locale;

public class BotCatalogActivity extends Activity {
 private FirebaseFirestore db; private LinearLayout list; private String filter="";
 @Override protected void onCreate(Bundle b){super.onCreate(b);db=FirebaseFirestore.getInstance();if(FirebaseAuth.getInstance().getCurrentUser()==null){finish();return;}show();}
 private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.BLACK);v.setPadding(10,10,10,10);return v;}
 private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(16);return b;}
 private void show(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,20,20,20);TextView title=text("🌐 Каталог ботов",28);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);EditText search=new EditText(this);search.setHint("Поиск бота");root.addView(search);Button find=button("🔎 Найти");root.addView(find);find.setOnClickListener(v->{filter=search.getText().toString().trim().toLowerCase(Locale.ROOT);load();});ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));Button back=button("← Назад");root.addView(back);back.setOnClickListener(v->finish());setContentView(root);load();}
 private void load(){db.collection("bots").orderBy("createdAt",Query.Direction.DESCENDING).limit(100).get().addOnSuccessListener(s->{list.removeAllViews();int count=0;for(DocumentSnapshot d:s.getDocuments()){String n=d.getString("name"),desc=d.getString("description");String name=n==null?"Бот":n;if(!filter.isEmpty()&&!name.toLowerCase(Locale.ROOT).contains(filter)&&!(desc==null?"":desc.toLowerCase(Locale.ROOT)).contains(filter))continue;count++;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(8,14,8,14);card.addView(text("🤖 "+name,20));if(desc!=null&&!desc.isEmpty())card.addView(text(desc,16));Button chat=button("💬 Открыть чат");card.addView(chat);list.addView(card);String id=d.getId();chat.setOnClickListener(v->{Intent i=new Intent(this,BotChatActivity.class);i.putExtra("botId",id);startActivity(i);});}if(count==0)list.addView(text(filter.isEmpty()?"В каталоге пока нет ботов.":"Ничего не найдено.",18));});}
}
