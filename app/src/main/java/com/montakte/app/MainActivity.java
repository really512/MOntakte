package com.montakte.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
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
            if (currentUser != null) {
                showLoggedInScreen(currentUser);
            } else {
                showPhoneScreen();
            }
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
        root.setPadding(40, 40, 40, 40);
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
            if (phone.length() < 7) {
                phoneInput.setError("Введите номер телефона");
                return;
            }
            sendCode(phone);
        });
    }

    private void sendCode(String phone) {
        Toast.makeText(this, "Отправляем код...", Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(MainActivity.this,
                                "Не удалось отправить код: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String id,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        resendToken = token;
                        showCodeScreen(phone);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showCodeScreen(String phone) {
        setupRoot();

        TextView title = text("Подтверждение", 30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        root.addView(text("Код подтверждения отправлен на", 17));
        root.addView(text(phone, 18));

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
            if (code.length() < 4) {
                codeInput.setError("Введите код из SMS");
                return;
            }
            if (verificationId == null) {
                codeInput.setError("Сначала запросите код ещё раз");
                return;
            }

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneAuthCredential(credential);
        });

        resendButton.setOnClickListener(v -> {
            if (resendToken == null) {
                sendCode(phone);
                return;
            }

            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setForceResendingToken(resendToken)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            signInWithPhoneAuthCredential(credential);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            Toast.makeText(MainActivity.this,
                                    "Не удалось отправить код: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull String id,
                                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            verificationId = id;
                            resendToken = token;
                            Toast.makeText(MainActivity.this,
                                    "Новый код отправлен",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            if (user != null) {
                                showLoggedInScreen(user);
                            }
                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(MainActivity.this,
                                        "Неверный код подтверждения",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Ошибка входа: " + task.getException(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void showLoggedInScreen(FirebaseUser user) {
        setupRoot();

        TextView title = text("Добро пожаловать в Контакте!", 28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        String phone = user.getPhoneNumber();
        root.addView(text(phone == null ? "Вход выполнен" : phone, 18));
    }

    private void showFirebaseSetupScreen() {
        setupRoot();

        TextView title = text("Контакте", 34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        root.addView(text("SMS-авторизация почти готова.", 20));
        root.addView(text("Нужно подключить Firebase-проект и файл google-services.json.", 16));
    }
}
