package com.montakte.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private LinearLayout root;
    private EditText phoneInput;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            auth = FirebaseAuth.getInstance(); db = FirebaseFirestore.getInstance();
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) { ensureUser(user); showHomeScreen(user); } else showPhoneScreen();
        } catch (IllegalStateException e) { showFirebaseSetupScreen(); }
    }
    private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.BLACK);v.setGravity(Gravity.CENTER);v.setPadding(8,12,8,12);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(16);return b;}
    private void setupRoot(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(32,32,32,32);setContentView(root);}
    private void addTitle(String s,float z){TextView t=text(s,z);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(t);}
    private void addBack(final Runnable r){Button b=button("← Назад");root.addView(b);b.setOnClickListener(v->r.run());}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    private void showPhoneScreen(){setupRoot();addTitle("Контакте",34);root.addView(text("Вход или регистрация",20));phoneInput=new EditText(this);phoneInput.setHint("Номер телефона, например +7...");phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);phoneInput.setSingleLine(true);root.addView(phoneInput);Button b=button("Получить код");root.addView(b);b.setOnClickListener(v->{String p=phoneInput.getText().toString().trim();if(p.length()<7)phoneInput.setError("Введите номер");else sendCode(p);});}
    private void sendCode(String phone){toast("Отправляем код...");PhoneAuthOptions o=PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone).setTimeout(60L,TimeUnit.SECONDS).setActivity(this).setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){@Override public void onVerificationCompleted(@NonNull PhoneAuthCredential c){signIn(c);}@Override public void onVerificationFailed(@NonNull FirebaseException e){toast("Не удалось отправить код: "+e.getMessage());}@Override public void onCodeSent(@NonNull String id,@NonNull PhoneAuthProvider.ForceResendingToken t){verificationId=id;resendToken=t;showCodeScreen(phone);}}).build();PhoneAuthProvider.verifyPhoneNumber(o);}
    private void showCodeScreen(String phone){setupRoot();addTitle("Подтверждение",30);root.addView(text("Код отправлен на\n"+phone,17));EditText c=new EditText(this);c.setHint("Код из SMS");c.setInputType(InputType.TYPE_CLASS_NUMBER);c.setSingleLine(true);root.addView(c);Button v=button("Подтвердить"),r=button("Отправить код ещё раз");root.addView(v);root.addView(r);v.setOnClickListener(x->{if(c.getText().length()<4)c.setError("Введите код");else if(verificationId==null)c.setError("Запросите код ещё раз");else signIn(PhoneAuthProvider.getCredential(verificationId,c.getText().toString().trim()));});r.setOnClickListener(x->sendCode(phone));}
    private void signIn(PhoneAuthCredential c){auth.signInWithCredential(c).addOnCompleteListener(this,t->{if(t.isSuccessful()){FirebaseUser u=t.getResult().getUser();if(u!=null){ensureUser(u);showHomeScreen(u);}}else if(t.getException() instanceof FirebaseAuthInvalidCredentialsException)toast("Неверный код");else toast("Ошибка входа: "+t.getException());});}
    private void ensureUser(FirebaseUser u){if(u.getPhoneNumber()==null)return;Map<String,Object> m=new HashMap<>();m.put("uid",u.getUid());m.put("phone",u.getPhoneNumber());m.put("updatedAt",FieldValue.serverTimestamp());db.collection("users").document(u.getUid()).set(m,com.google.firebase.firestore.SetOptions.merge());}

    private void showHomeScreen(FirebaseUser u){setupRoot();root.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);addTitle("Контакте",32);root.addView(text(u.getPhoneNumber()==null?"Аккаунт":u.getPhoneNumber(),15));Button f=button("👥 Друзья"),q=button("📨 Заявки в друзья"),m=button("💬 Сообщения"),p=button("👤 Профиль");root.addView(f);root.addView(q);root.addView(m);root.addView(p);root.addView(text("Лента\n\nПосты подключим следующим этапом.",20));f.setOnClickListener(v->showFriendsScreen(u));q.setOnClickListener(v->showRequestsScreen(u));m.setOnClickListener(v->showMessagesScreen(u));p.setOnClickListener(v->showProfileScreen(u));}

    private void showFriendsScreen(FirebaseUser u){setupRoot();addTitle("Друзья",30);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);addBack(()->showHomeScreen(u));loadFriends(u,list);Button add=button("＋ Добавить друга");root.addView(add);add.setOnClickListener(v->showFindUserScreen(u));}
    private void loadFriends(FirebaseUser u,LinearLayout list){db.collection("friendships").whereEqualTo("userA",u.getUid()).get().addOnSuccessListener(s->renderFriends(s.getDocuments(),u,list));db.collection("friendships").whereEqualTo("userB",u.getUid()).get().addOnSuccessListener(s->renderFriends(s.getDocuments(),u,list));}
    private void renderFriends(List<DocumentSnapshot> docs,FirebaseUser u,LinearLayout list){for(DocumentSnapshot d:docs){String a=d.getString("userA"),b=d.getString("userB");String other=u.getUid().equals(a)?b:a;if(other!=null)db.collection("users").document(other).get().addOnSuccessListener(x->{String ph=x.getString("phone");if(ph!=null)list.addView(text("👤 "+ph,18));});}}

    private void showRequestsScreen(FirebaseUser u){setupRoot();addTitle("Заявки в друзья",30);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);db.collection("friendRequests").whereEqualTo("receiverUid",u.getUid()).whereEqualTo("status","pending").get().addOnSuccessListener(s->{if(s.isEmpty())list.addView(text("Новых заявок нет",18));for(DocumentSnapshot d:s.getDocuments())renderRequest(d,u,list);});addBack(()->showHomeScreen(u));}
    private void renderRequest(DocumentSnapshot d,FirebaseUser u,LinearLayout list){String sender=d.getString("senderUid");if(sender==null)return;db.collection("users").document(sender).get().addOnSuccessListener(x->{String ph=x.getString("phone");LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.addView(text("Заявка от "+(ph==null?sender:ph),18));Button a=button("Принять"),r=button("Отклонить");row.addView(a);row.addView(r);list.addView(row);a.setOnClickListener(v->acceptRequest(d.getId(),sender,u));r.setOnClickListener(v->db.collection("friendRequests").document(d.getId()).update("status","rejected").addOnSuccessListener(z->showRequestsScreen(u)));});}
    private void acceptRequest(String id,String sender,FirebaseUser u){db.collection("friendRequests").document(id).update("status","accepted").addOnSuccessListener(v->{String key=friendshipId(sender,u.getUid());Map<String,Object> m=new HashMap<>();List<String> ids=new ArrayList<>(Arrays.asList(sender,u.getUid()));Collections.sort(ids);m.put("userA",ids.get(0));m.put("userB",ids.get(1));m.put("createdAt",FieldValue.serverTimestamp());db.collection("friendships").document(key).set(m).addOnSuccessListener(x->{toast("Заявка принята");showRequestsScreen(u);});});}
    private void showFindUserScreen(FirebaseUser u){setupRoot();addTitle("Добавить в друзья",30);EditText s=new EditText(this);s.setHint("Номер телефона пользователя");s.setInputType(InputType.TYPE_CLASS_PHONE);s.setSingleLine(true);root.addView(s);Button b=button("Отправить заявку");root.addView(b);addBack(()->showFriendsScreen(u));b.setOnClickListener(v->{String ph=s.getText().toString().trim();if(ph.length()<7){s.setError("Введите номер");return;}db.collection("users").whereEqualTo("phone",ph).limit(1).get().addOnSuccessListener(x->{if(x.isEmpty()){toast("Пользователь не найден");return;}String other=x.getDocuments().get(0).getId();if(other.equals(u.getUid())){toast("Нельзя добавить самого себя");return;}String id=u.getUid()+"_"+other;Map<String,Object> m=new HashMap<>();m.put("senderUid",u.getUid());m.put("receiverUid",other);m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("friendRequests").document(id).set(m).addOnSuccessListener(z->toast("Заявка отправлена"));});});}

    private void showMessagesScreen(FirebaseUser u){setupRoot();addTitle("Сообщения",30);Button n=button("＋ Новый чат");root.addView(n);n.setOnClickListener(v->showNewMessageScreen(u));LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);addBack(()->showHomeScreen(u));db.collection("conversations").whereArrayContains("participants",u.getUid()).get().addOnSuccessListener(s->{if(s.isEmpty())list.addView(text("Диалогов пока нет",18));for(DocumentSnapshot d:s.getDocuments()){List<String> p=(List<String>)d.get("participants");if(p==null)continue;for(String id:p)if(!id.equals(u.getUid())){String other=id;db.collection("users").document(other).get().addOnSuccessListener(x->{Button b=button("💬 "+(x.getString("phone")==null?other:x.getString("phone")));list.addView(b);b.setOnClickListener(v->showChatScreen(u,other));});break;}}});}
    private void showNewMessageScreen(FirebaseUser u){setupRoot();addTitle("Новый чат",30);EditText r=new EditText(this);r.setHint("Телефон получателя");r.setInputType(InputType.TYPE_CLASS_PHONE);r.setSingleLine(true);root.addView(r);Button b=button("Открыть чат");root.addView(b);addBack(()->showMessagesScreen(u));b.setOnClickListener(v->db.collection("users").whereEqualTo("phone",r.getText().toString().trim()).limit(1).get().addOnSuccessListener(s->{if(s.isEmpty())toast("Пользователь не найден");else{String other=s.getDocuments().get(0).getId();if(!other.equals(u.getUid()))showChatScreen(u,other);}}));}
    private String friendshipId(String a,String b){List<String> x=new ArrayList<>(Arrays.asList(a,b));Collections.sort(x);return x.get(0)+"_"+x.get(1);}
    private void showChatScreen(FirebaseUser u,String other){setupRoot();addTitle("Чат",30);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(this);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout composer=new LinearLayout(this);EditText input=new EditText(this);input.setHint("Сообщение");Button send=button("Отправить");composer.addView(input,new LinearLayout.LayoutParams(0,-2,1));composer.addView(send);root.addView(composer);addBack(()->showMessagesScreen(u));String cid=friendshipId(u.getUid(),other);Map<String,Object> c=new HashMap<>();c.put("participants",Arrays.asList(u.getUid(),other));db.collection("conversations").document(cid).set(c,com.google.firebase.firestore.SetOptions.merge());Query q=db.collection("conversations").document(cid).collection("messages").orderBy("createdAt",Query.Direction.ASCENDING);q.addSnapshotListener((snap,e)->{if(e!=null||snap==null)return;list.removeAllViews();for(DocumentSnapshot d:snap.getDocuments()){String body=d.getString("text"),sender=d.getString("senderUid");list.addView(text((u.getUid().equals(sender)?"Вы: ":"Собеседник: ")+(body==null?"":body),17));}scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));});send.setOnClickListener(v->{String body=input.getText().toString().trim();if(body.isEmpty())return;Map<String,Object> m=new HashMap<>();m.put("senderUid",u.getUid());m.put("text",body);m.put("createdAt",FieldValue.serverTimestamp());db.collection("conversations").document(cid).collection("messages").add(m).addOnSuccessListener(x->input.setText(""));});}

    private void showProfileScreen(FirebaseUser u){setupRoot();addTitle("Профиль",30);root.addView(text("Телефон",16));root.addView(text(u.getPhoneNumber()==null?"Не указан":u.getPhoneNumber(),18));addBack(()->showHomeScreen(u));}
    private void showFirebaseSetupScreen(){setupRoot();addTitle("Контакте",34);root.addView(text("Нужно подключить Firebase-проект и google-services.json.",18));}
}
