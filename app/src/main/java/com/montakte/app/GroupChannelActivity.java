package com.montakte.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GroupChannelActivity extends Activity {
    private LinearLayout root; private FirebaseFirestore db; private FirebaseAuth auth;
    @Override protected void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();showCreateScreen();}
    private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.BLACK);v.setPadding(8,10,8,10);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(16);return b;}
    private void base(String title){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);TextView t=text(title,28);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);root.addView(t);}
    private void showCreateScreen(){
        FirebaseUser user=auth.getCurrentUser();if(user==null){Toast.makeText(this,"Сначала войдите в ВОнтакте",Toast.LENGTH_LONG).show();finish();return;}
        base("Создать группу или канал");root.addView(text("Выберите тип:",18));
        RadioGroup typeGroup=new RadioGroup(this);RadioButton group=new RadioButton(this);group.setText("👥 Группа — общение участников");group.setTextSize(17);group.setId(1001);RadioButton channel=new RadioButton(this);channel.setText("📢 Канал — публикации владельца и админов");channel.setTextSize(17);channel.setId(1002);typeGroup.addView(group);typeGroup.addView(channel);group.setChecked(true);root.addView(typeGroup);
        String requested=getIntent().getStringExtra("communityType");if("channel".equals(requested))channel.setChecked(true);else if("group".equals(requested))group.setChecked(true);
        EditText name=new EditText(this);name.setHint("Название");name.setSingleLine(true);root.addView(name);EditText description=new EditText(this);description.setHint("Описание (необязательно)");description.setMinLines(3);description.setGravity(Gravity.TOP);root.addView(description);
        RadioGroup visibility=new RadioGroup(this);RadioButton pub=new RadioButton(this);pub.setText("🌐 Публичная");pub.setTextSize(17);RadioButton priv=new RadioButton(this);priv.setText("🔒 Приватная");priv.setTextSize(17);visibility.addView(pub);visibility.addView(priv);pub.setChecked(true);root.addView(text("Доступ:",18));root.addView(visibility);
        Button create=button("Создать");root.addView(create);Button back=button("← Назад");root.addView(back);
        create.setOnClickListener(v->{String n=name.getText().toString().trim(),d=description.getText().toString().trim();if(n.isEmpty()){name.setError("Введите название");return;}if(n.length()>80){name.setError("Название слишком длинное");return;}if(d.length()>500){description.setError("Описание слишком длинное");return;}boolean isChannel=channel.isChecked(),isPublic=pub.isChecked();Map<String,Object> c=new HashMap<>();c.put("type",isChannel?"channel":"group");c.put("name",n);c.put("description",d);c.put("ownerUid",user.getUid());c.put("admins",new ArrayList<>(Arrays.asList(user.getUid())));c.put("members",new ArrayList<>(Arrays.asList(user.getUid())));c.put("isPublic",isPublic);c.put("createdAt",FieldValue.serverTimestamp());create.setEnabled(false);db.collection("communities").add(c).addOnSuccessListener(doc->{Toast.makeText(this,(isChannel?"Канал":"Группа")+" создана!",Toast.LENGTH_LONG).show();finish();}).addOnFailureListener(e->{create.setEnabled(true);Toast.makeText(this,"Ошибка создания: "+e.getMessage(),Toast.LENGTH_LONG).show();});});
        back.setOnClickListener(v->finish());
    }
}
