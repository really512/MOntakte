package com.montakte.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

public class BotChatActivity extends Activity {
 private FirebaseFirestore db; private FirebaseUser user; private String botId,botName; private LinearLayout messages; private EditText input;
 @Override protected void onCreate(Bundle b){super.onCreate(b);db=FirebaseFirestore.getInstance();user=FirebaseAuth.getInstance().getCurrentUser();botId=getIntent().getStringExtra("botId");if(user==null||botId==null){finish();return;}loadBot();}
 private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.BLACK);v.setPadding(12,9,12,9);return v;}
 private GradientDrawable bubble(int c){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(26);return g;}
 private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(15);return b;}
 private void loadBot(){db.collection("bots").document(botId).get().addOnSuccessListener(d->{if(!d.exists()){finish();return;}botName=d.getString("name");show();}).addOnFailureListener(e->{Toast.makeText(this,"Не удалось открыть бота",Toast.LENGTH_SHORT).show();finish();});}
 private void show(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,12,12,12);TextView title=text("🤖 "+(botName==null?"Бот":botName),25);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);ScrollView sc=new ScrollView(this);messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);messages.setPadding(4,8,4,8);sc.addView(messages);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));input=new EditText(this);input.setHint("Напишите боту...");Button send=button("Отправить");LinearLayout bar=new LinearLayout(this);bar.addView(input,new LinearLayout.LayoutParams(0,-2,1));bar.addView(send);root.addView(bar);Button back=button("← Назад");root.addView(back);back.setOnClickListener(v->finish());send.setOnClickListener(v->send());setContentView(root);loadMessages();}
 private void loadMessages(){db.collection("bots").document(botId).collection("messages").orderBy("createdAt",Query.Direction.ASCENDING).limitToLast(100).addSnapshotListener((s,e)->{if(e!=null||s==null)return;messages.removeAllViews();for(DocumentSnapshot d:s.getDocuments())addBubble(d.getString("text"),Boolean.TRUE.equals(d.getBoolean("fromBot")));});}
 private void addBubble(String s,boolean bot){TextView v=text(s==null?"":s,16);v.setBackground(bubble(bot?Color.rgb(240,240,240):Color.rgb(220,240,255)));v.setGravity(bot?Gravity.LEFT:Gravity.RIGHT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.gravity=bot?Gravity.LEFT:Gravity.RIGHT;p.setMargins(8,5,8,5);messages.addView(v,p);}
 private void send(){String s=input.getText().toString().trim();if(s.isEmpty())return;input.setText("");Map<String,Object> m=new HashMap<>();m.put("senderUid",user.getUid());m.put("text",s);m.put("fromBot",false);m.put("createdAt",FieldValue.serverTimestamp());db.collection("bots").document(botId).collection("messages").add(m).addOnSuccessListener(x->replyFor(s)).addOnFailureListener(e->Toast.makeText(this,"Не удалось отправить",Toast.LENGTH_SHORT).show());}
 private void replyFor(String s){String command=s.trim().toLowerCase();db.collection("bots").document(botId).collection("commands").whereEqualTo("trigger",command).limit(1).get().addOnSuccessListener(q->{String reply;if(!q.isEmpty())reply=q.getDocuments().get(0).getString("reply");else reply=defaultReply(command);Map<String,Object> r=new HashMap<>();r.put("senderUid",botId);r.put("text",reply);r.put("fromBot",true);r.put("createdAt",FieldValue.serverTimestamp());db.collection("bots").document(botId).collection("messages").add(r);});}
 private String defaultReply(String s){if(s.equals("/start"))return "Привет! 🤖 Напиши /help или используй команды, которые настроил владелец.";if(s.equals("/help"))return "Команда не настроена. Владелец может добавить её в ⚙ Команды.";return "Команда не найдена 🤖";}
}
