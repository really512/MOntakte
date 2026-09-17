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
import java.util.List;
import java.util.Map;

public class CommunityChatActivity extends Activity {
    private FirebaseFirestore db; private FirebaseUser user; private String communityId,type,ownerUid,name;
    private List<String> admins,members; private LinearLayout messages; private EditText input; private Button send;
    @Override protected void onCreate(Bundle b){super.onCreate(b);db=FirebaseFirestore.getInstance();user=FirebaseAuth.getInstance().getCurrentUser();communityId=getIntent().getStringExtra("communityId");if(user==null||communityId==null){finish();return;}loadCommunity();}
    private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.BLACK);v.setPadding(10,10,10,10);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(16);return b;}
    private void loadCommunity(){db.collection("communities").document(communityId).get().addOnSuccessListener(d->{if(!d.exists()){finish();return;}type=d.getString("type");name=d.getString("name");ownerUid=d.getString("ownerUid");admins=(List<String>)d.get("admins");members=(List<String>)d.get("members");showChat(name==null?"Сообщество":name);}).addOnFailureListener(e->{Toast.makeText(this,"Не удалось открыть сообщество",Toast.LENGTH_LONG).show();finish();});}
    private boolean isMember(){return members!=null&&members.contains(user.getUid());}
    private boolean isAdmin(){return user.getUid().equals(ownerUid)||(admins!=null&&admins.contains(user.getUid()));}
    private void showChat(String titleName){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,16,16,16);
        TextView title=text(("channel".equals(type)?"📢 ":"👥 ")+titleName,25);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView status=text(isMember()?"Вы участник":"Вы не участник",14);root.addView(status);
        Button membership=button(isMember()?"Выйти":"Вступить");root.addView(membership);membership.setOnClickListener(v->toggleMembership(membership,status));
        ScrollView scroll=new ScrollView(this);messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        input=new EditText(this);input.setHint("Сообщение");input.setSingleLine(false);send=button("Отправить");LinearLayout bar=new LinearLayout(this);bar.addView(input,new LinearLayout.LayoutParams(0,-2,1));bar.addView(send);root.addView(bar);
        if(!isMember()||("channel".equals(type)&&!isAdmin())){input.setEnabled(false);send.setEnabled(false);input.setHint("Вступите в сообщество, чтобы писать");if("channel".equals(type)&&isMember())input.setHint("Писать могут только админы");}
        Button back=button("← Назад");root.addView(back);back.setOnClickListener(v->finish());send.setOnClickListener(v->sendMessage());setContentView(root);loadMessages();
    }
    private void toggleMembership(Button b,TextView status){
        boolean join=!isMember(); if("channel".equals(type)&&!join&&isAdmin()){Toast.makeText(this,"Владелец/админ не может выйти из канала",Toast.LENGTH_SHORT).show();return;}
        db.collection("communities").document(communityId).update("members",join?FieldValue.arrayUnion(user.getUid()):FieldValue.arrayRemove(user.getUid())).addOnSuccessListener(v->{if(join){if(members==null)members=new java.util.ArrayList<>();members.add(user.getUid());}else if(members!=null)members.remove(user.getUid());b.setText(join?"Выйти":"Вступить");status.setText(join?"Вы участник":"Вы не участник");input.setEnabled(join&&(!"channel".equals(type)||isAdmin()));send.setEnabled(join&&(!"channel".equals(type)||isAdmin()));}).addOnFailureListener(e->Toast.makeText(this,"Не удалось изменить участие",Toast.LENGTH_SHORT).show());
    }
    private void loadMessages(){db.collection("communities").document(communityId).collection("messages").orderBy("createdAt",Query.Direction.ASCENDING).limitToLast(100).addSnapshotListener((snap,e)->{if(e!=null||snap==null)return;messages.removeAllViews();for(DocumentSnapshot d:snap.getDocuments()){String sender=d.getString("senderUid"),body=d.getString("text");messages.addView(text("👤 "+(sender==null?"Пользователь":sender)+"\n"+(body==null?"":body),16));}});}
    private void sendMessage(){String s=input.getText().toString().trim();if(s.isEmpty())return;send.setEnabled(false);Map<String,Object>m=new HashMap<>();m.put("senderUid",user.getUid());m.put("text",s);m.put("createdAt",FieldValue.serverTimestamp());db.collection("communities").document(communityId).collection("messages").add(m).addOnSuccessListener(x->{input.setText("");send.setEnabled(true);}).addOnFailureListener(e->{send.setEnabled(true);Toast.makeText(this,"Не удалось отправить: "+e.getMessage(),Toast.LENGTH_LONG).show();});}
}
