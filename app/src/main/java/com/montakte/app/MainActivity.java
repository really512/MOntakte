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

public class MainActivity extends Activity {
    private LinearLayout root;
    private EditText phoneInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPhoneScreen();
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

        TextView subtitle = text("Вход или регистрация", 20);
        root.addView(subtitle);

        phoneInput = new EditText(this);
        phoneInput.setHint("Номер телефона, например +7...");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setSingleLine(true);
        root.addView(phoneInput, new LinearLayout.LayoutParams(-1, -2));

        Button continueButton = button("Получить код");
        root.addView(continueButton);

        continueButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            if (phone.length() >= 7) {
                showCodeScreen(phone);
            } else {
                phoneInput.setError("Введите номер телефона");
            }
        });
    }

    private void showCodeScreen(String phone) {
        setupRoot();

        TextView title = text("Подтверждение", 30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        root.addView(text("Код подтверждения будет отправлен на", 17));
        root.addView(text(phone, 18));

        EditText codeInput = new EditText(this);
        codeInput.setHint("Введите код из SMS");
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setSingleLine(true);
        codeInput.setGravity(Gravity.CENTER);
        root.addView(codeInput, new LinearLayout.LayoutParams(-1, -2));

        Button verifyButton = button("Подтвердить");
        root.addView(verifyButton);

        TextView note = text("Реальная отправка SMS подключается через сервер авторизации.", 14);
        root.addView(note);

        verifyButton.setOnClickListener(v -> {
            if (codeInput.getText().length() < 4) {
                codeInput.setError("Введите код из SMS");
            }
        });
    }
}
