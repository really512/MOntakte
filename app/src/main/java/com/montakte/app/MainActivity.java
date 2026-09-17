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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private LinearLayout root;
    private EditText phoneInput;
    private FirebaseAuth auth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) showHomeScreen(currentUser);
            else showPhoneScreen();
        } catch (IllegalStateException e) {
            showFirebaseSetupScreen();
        }
    }

    private TextView text(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.BLACK);
        view.setGravity(Gravity.CENTER);
        view.setPadding(8, 12, 8, 12);
        return view;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(16);
        return button;
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        setContentView(root);
    }

    private void showPhoneScreen() {
        setupRoot();
        TextView title = text("Контакте", 34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Вход или регистрация", 20));
        phoneInput = new EditText(this);
        phoneInput.setHint("Номер телефона, например +7...");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setSingleLine(true);
        root.addView(phoneInput, new LinearLayout.LayoutParams(-1, -2));
        Button continueButton = button("Получить код");
        root.addView(continueButton);
        continueButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            if (phone.length() < 7) phoneInput.setError("Введите номер телефона");
            else sendCode(phone);
        });
    }

    private void sendCode(String phone) {
        Toast.makeText(this, "Отправляем код...", Toast.LENGTH_SHORT).show();
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) { signInWithPhoneAuthCredential(credential); }
                    @Override public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(MainActivity.this, "Не удалось отправить код: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    @Override public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        resendToken = token;
                        showCodeScreen(phone);
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showCodeScreen(String phone) {
        setupRoot();
        TextView title = text("Подтверждение", 30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Код подтверждения отправлен на\n" + phone, 17));
        EditText codeInput = new EditText(this);
        codeInput.setHint("Введите код из SMS");
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setSingleLine(true);
        codeInput.setGravity(Gravity.CENTER);
        root.addView(codeInput, new LinearLayout.LayoutParams(-1, -2));
        Button verifyButton = button("Подтвердить");
        root.addView(verifyButton);
        Button resendButton = button("Отправить код ещё раз");
        root.addView(resendButton);
        verifyButton.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (code.length() < 4) { codeInput.setError("Введите код из SMS"); return; }
            if (verificationId == null) { codeInput.setError("Сначала запросите код ещё раз"); return; }
            signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(verificationId, code));
        });
        resendButton.setOnClickListener(v -> sendCode(phone));
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = task.getResult().getUser();
                    if (user != null) showHomeScreen(user);
                } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(MainActivity.this, "Неверный код подтверждения", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка входа: " + task.getException(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showHomeScreen(FirebaseUser user) {
        setupRoot();
        root.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        TextView header = text("Контакте", 32);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        root.addView(text(user.getPhoneNumber() == null ? "Аккаунт" : user.getPhoneNumber(), 15));

        Button profile = button("👤 Профиль");
        Button friends = button("👥 Друзья");
        Button requests = button("📨 Заявки в друзья");
        Button messages = button("💬 Сообщения");
        Button createPost = button("＋ Создать пост");
        root.addView(profile);
        root.addView(friends);
        root.addView(requests);
        root.addView(messages);
        root.addView(createPost);

        View divider = new View(this);
        divider.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, 2);
        dp.setMargins(0, 18, 0, 18);
        root.addView(divider, dp);

        TextView feed = text("Лента\n\nПока здесь ничего нет", 22);
        root.addView(feed);

        profile.setOnClickListener(v -> showProfileScreen(user));
        friends.setOnClickListener(v -> showFriendsScreen(user));
        requests.setOnClickListener(v -> showRequestsScreen(user));
        messages.setOnClickListener(v -> showMessagesScreen(user));
        createPost.setOnClickListener(v -> showCreatePostScreen());
    }

    private void showFriendsScreen(FirebaseUser user) {
        setupRoot();
        addTitle("Друзья", 30);
        root.addView(text("Здесь будут ваши друзья.", 18));
        root.addView(text("Пока список пуст — добавление друзей подключим к серверной базе.", 16));
        addBackButton(() -> showHomeScreen(user));
    }

    private void showRequestsScreen(FirebaseUser user) {
        setupRoot();
        addTitle("Заявки в друзья", 30);
        root.addView(text("Входящие заявки", 20));
        root.addView(text("Пока нет новых заявок.", 17));
        Button demo = button("＋ Найти пользователя и отправить заявку");
        root.addView(demo);
        demo.setOnClickListener(v -> showFindUserScreen(user));
        addBackButton(() -> showHomeScreen(user));
    }

    private void showFindUserScreen(FirebaseUser user) {
        setupRoot();
        addTitle("Добавить в друзья", 30);
        EditText search = new EditText(this);
        search.setHint("Номер телефона пользователя");
        search.setInputType(InputType.TYPE_CLASS_PHONE);
        search.setSingleLine(true);
        root.addView(search);
        Button send = button("Отправить заявку");
        root.addView(send);
        send.setOnClickListener(v -> {
            if (search.getText().toString().trim().length() < 7) search.setError("Введите номер");
            else Toast.makeText(this, "Заявка будет отправлена после подключения базы пользователей.", Toast.LENGTH_LONG).show();
        });
        addBackButton(() -> showRequestsScreen(user));
    }

    private void showMessagesScreen(FirebaseUser user) {
        setupRoot();
        addTitle("Сообщения", 30);
        root.addView(text("💬 Ваши сообщения", 21));
        root.addView(text("Диалогов пока нет.", 17));
        Button newMessage = button("＋ Новое сообщение");
        root.addView(newMessage);
        newMessage.setOnClickListener(v -> showNewMessageScreen(user));
        addBackButton(() -> showHomeScreen(user));
    }

    private void showNewMessageScreen(FirebaseUser user) {
        setupRoot();
        addTitle("Новое сообщение", 30);
        EditText recipient = new EditText(this);
        recipient.setHint("Телефон получателя");
        recipient.setInputType(InputType.TYPE_CLASS_PHONE);
        recipient.setSingleLine(true);
        root.addView(recipient);
        EditText message = new EditText(this);
        message.setHint("Сообщение");
        message.setMinLines(4);
        message.setGravity(Gravity.TOP);
        root.addView(message);
        Button send = button("Отправить");
        root.addView(send);
        send.setOnClickListener(v -> {
            if (recipient.getText().toString().trim().length() < 7) recipient.setError("Введите номер");
            else if (message.getText().toString().trim().isEmpty()) message.setError("Введите сообщение");
            else Toast.makeText(this, "Сообщения будут отправляться через серверную базу на следующем этапе.", Toast.LENGTH_LONG).show();
        });
        addBackButton(() -> showMessagesScreen(user));
    }

    private void showProfileScreen(FirebaseUser user) {
        setupRoot();
        addTitle("Профиль", 30);
        root.addView(text("Телефон", 16));
        root.addView(text(user.getPhoneNumber() == null ? "Не указан" : user.getPhoneNumber(), 18));
        addBackButton(() -> showHomeScreen(user));
    }

    private void showCreatePostScreen() {
        setupRoot();
        addTitle("Создать пост", 30);
        EditText postInput = new EditText(this);
        postInput.setHint("Что нового?");
        postInput.setGravity(Gravity.TOP);
        postInput.setMinLines(5);
        postInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        root.addView(postInput, new LinearLayout.LayoutParams(-1, -2));
        Button publish = button("Опубликовать");
        root.addView(publish);
        publish.setOnClickListener(v -> {
            if (postInput.getText().toString().trim().isEmpty()) postInput.setError("Напишите текст поста");
            else Toast.makeText(this, "Пост подготовлен. Серверная лента будет подключена следующим этапом.", Toast.LENGTH_LONG).show();
        });
        Button back = button("← Назад");
        root.addView(back);
        back.setOnClickListener(v -> { FirebaseUser user = auth.getCurrentUser(); if (user != null) showHomeScreen(user); });
    }

    private void addTitle(String value, float size) {
        TextView title = text(value, size);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
    }

    private void addBackButton(final Runnable action) {
        Button back = button("← Назад");
        root.addView(back);
        back.setOnClickListener(v -> action.run());
    }

    private void showFirebaseSetupScreen() {
        setupRoot();
        addTitle("Контакте", 34);
        root.addView(text("SMS-авторизация почти готова.\nНужно подключить Firebase-проект и файл google-services.json.", 18));
    }
}
