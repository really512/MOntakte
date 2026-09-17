package com.montakte.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityChatActivity extends Activity {
    private FirebaseFirestore db; private FirebaseUser user; private String communityId,type,ownerUid,name;
    private List<String> admins,members; private LinearLayout messages; private EditText input; private Button send;
    @Override protected void onCreate(Bundle b){super.onCreate(b);db=FirebaseFirestore.getInstance();user=FirebaseAuth.getInstance().getCurrentUser();communityId=getIntent().getStringExtra("communityId");if(user==null||communityId==null){finish();return;}loadCommunity();}
    private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.BLACK);v.setPadding(12,8,12,8);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(16);return b;}
    private GradientDrawable bubble(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(28);return g;}
    private void loadCommunity(){db.collection("communities").document(communityId).get().addOnSuccessListener(d->{if(!d.exists()){finish();return;}type=d.getString("type");name=d.getString("name");ownerUid=d.getString("ownerUid");admins=(List<String>)d.get("admins");members=(List<String>)d.get("members");showChat(name==null?"Сообщество":name);}).addOnFailureListener(e->{Toast.makeText(this,"Не удалось открыть сообщество",Toast.LENGTH_LONG).show();finish();});}
    private boolean isMember(){return members!=null&&members.contains(user.getUid());}
    private boolean isAdmin(){return user.getUid().equals(ownerUid)||(admins!=null&&admins.contains(user.getUid()));}
    private void showChat(String titleName){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,12,12,12);
        TextView title=text(("channel".equals(type)?"📢 ":"👥 ")+titleName,25);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView status=text(isMember()?"Вы участник":"Вы не участник",14);status.setGravity(Gravity.CENTER);root.addView(status);
        Button membership=button(isMember()?"Выйти":"Вступить");root.addView(membership);membership.setOnClickListener(v->toggleMembership(membership,status));
        ScrollView scroll=new ScrollView(this);messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);messages.setPadding(4,12,4,12);scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        input=new EditText(this);input.setHint("Сообщение");input.setSingleLine(false);send=button("Отправить");LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.addView(input,new LinearLayout.LayoutParams(0,-2,1));bar.addView(send);root.addView(bar);
        if(!isMember()||("channel".equals(type)&&!isAdmin())){input.setEnabled(false);send.setEnabled(false);input.setHint("Вступите в сообщество, чтобы писать");if("channel".equals(type)&&isMember())input.setHint("Писать могут только админы");}
        Button back=button("← Назад");root.addView(back);back.setOnClickListener(v->finish());send.setOnClickListener(v->sendMessage());setContentView(root);loadMessages();
    }
    private void toggleMembership(Button b,TextView status){boolean join=!isMember();if("channel".equals(type)&&!join&&isAdmin()){Toast.makeText(this,"Владелец/админ не может выйти из канала",Toast.LENGTH_SHORT).show();return;}db.collection("communities").document(communityId).update("members",join?FieldValue.arrayUnion(user.getUid()):FieldValue.arrayRemove(user.getUid())).addOnSuccessListener(v->{if(join){if(members==null)members=new ArrayList<>();if(!members.contains(user.getUid()))members.add(user.getUid());}else if(members!=null)members.remove(user.getUid());b.setText(join?"Выйти":"Вступить");status.setText(join?"Вы участник":"Вы не участник");input.setEnabled(join&&(!"channel".equals(type)||isAdmin()));send.setEnabled(join&&(!"channel".equals(type)||isAdmin()));}).addOnFailureListener(e->Toast.makeText(this,"Не удалось изменить участие",Toast.LENGTH_SHORT).show());}
    private void loadMessages(){db.collection("communities").document(communityId).collection("messages").orderBy("createdAt",Query.Direction.ASCENDING).limitToLast(100).addSnapshotListener((snap,e)->{if(e!=null||snap==null)return;messages.removeAllViews();for(DocumentSnapshot d:snap.getDocuments()){String sender=d.getString("senderUid"),body=d.getString("text");renderMessage(sender,body);}});}
    private void renderMessage(String uid,String body){
        final boolean mine=user.getUid().equals(uid); LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(mine?Gravity.RIGHT:Gravity.LEFT);row.setPadding(4,5,4,5);
        ImageView avatar=new ImageView(this);avatar.setImageResource(android.R.drawable.ic_menu_myplaces);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);GradientDrawable avBg=new GradientDrawable();avBg.setColor(Color.LTGRAY);avBg.setShape(GradientDrawable.OVAL);avatar.setBackground(avBg);avatar.setPadding(5,5,5,5);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(52,52);ap.gravity=Gravity.CENTER_VERTICAL;
        LinearLayout bubbleBox=new LinearLayout(this);bubbleBox.setOrientation(LinearLayout.VERTICAL);bubbleBox.setPadding(14,9,14,9);bubbleBox.setBackground(bubble(mine?Color.rgb(220,240,255):Color.rgb(240,240,240)));
        TextView who=text("Загрузка...",14);who.setTypeface(Typeface.DEFAULT,Typeface.BOLD);TextView msg=text(body==null?"":body,16);msg.setTextColor(Color.BLACK);bubbleBox.addView(who);bubbleBox.addView(msg);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.setMargins(8,0,8,0);row.addView(mine?bubbleBox:avatar,mine?bp:ap);row.addView(mine?avatar:bubbleBox,mine?ap:bp);messages.addView(row,new LinearLayout.LayoutParams(-1,-2));
        if(uid==null){who.setText("Пользователь");return;}db.collection("users").document(uid).get().addOnSuccessListener(p->{String n=p.getString("displayName"),phone=p.getString("phone"),avatarUrl=p.getString("avatar");if(n==null||n.trim().isEmpty())n=phone==null?"Пользователь":phone;who.setText(n);if(avatarUrl!=null&&!avatarUrl.trim().isEmpty()&&avatarUrl.startsWith("http"))com.bumptech.glide.Glide.with(this).load(avatarUrl).circleCrop().into(avatar);}).addOnFailureListener(e->who.setText("Пользователь"));
    }
    private void sendMessage(){String s=input.getText().toString().trim();if(s.isEmpty())return;send.setEnabled(false);Map<String,Object>m=new HashMap<>();m.put("senderUid",user.getUid());m.put("text",s);m.put("createdAt",FieldValue.serverTimestamp());db.collection("communities").document(communityId).collection("messages").add(m).addOnSuccessListener(x->{input.setText("");send.setEnabled(true);}).addOnFailureListener(e->{send.setEnabled(true);Toast.makeText(this,"Не удалось отправить: "+e.getMessage(),Toast.LENGTH_LONG).show();});}
}
