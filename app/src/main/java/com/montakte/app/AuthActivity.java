package com.montakte.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.regex.Pattern;

/** Email/password authentication entry point for ВОнтакте. */
public class AuthActivity extends Activity {
    private FirebaseAuth auth;
    private LinearLayout root;
    private final String accent = "#2684FF";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) { openApp(); return; }
        showAuth();
    }

    private GradientDrawable bg(String color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(radius);
        return g;
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(0, 4, 0, 4);
        return v;
    }

    private TextView center(String value, float size, int color) {
        TextView v = text(value, size, color);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private EditText input(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.parseColor("#667085"));
        e.setTextColor(Color.parseColor("#14213D"));
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setInputType(type);
        e.setPadding(18, 0, 18, 0);
        e.setBackground(bg("#F1F5FA", 22));
        return e;
    }

    /** TextView-style button so the label is always explicitly rendered above the blue background. */
    private TextView button(String label) {
        TextView b = center(label, 16, Color.WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setClickable(true);
        b.setFocusable(true);
        b.setPadding(16, 0, 16, 0);
        b.setBackground(bg(accent, 28));
        b.setMinHeight(52);
        return b;
    }

    private void showAuth() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28, 24, 28, 24);
        root.setBackgroundColor(Color.parseColor("#F5F7FB"));
        setContentView(root);

        TextView logo = center("ВО", 42, Color.WHITE);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setBackground(bg(accent, 32));
        root.addView(logo, new LinearLayout.LayoutParams(104, 104));

        Space s = new Space(this);
        root.addView(s, new LinearLayout.LayoutParams(1, 16));

        TextView title = center("Добро пожаловать в ВОнтакте", 24, Color.parseColor("#14213D"));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(center("Вход и регистрация по email", 15, Color.parseColor("#667085")));

        Space s2 = new Space(this);
        root.addView(s2, new LinearLayout.LayoutParams(1, 22));

        EditText email = input("Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        root.addView(email, new LinearLayout.LayoutParams(-1, 54));

        Space s3 = new Space(this);
        root.addView(s3, new LinearLayout.LayoutParams(1, 12));

        EditText password = input("Пароль", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(password, new LinearLayout.LayoutParams(-1, 54));

        Space s4 = new Space(this);
        root.addView(s4, new LinearLayout.LayoutParams(1, 14));

        TextView login = button("Войти");
        root.addView(login, new LinearLayout.LayoutParams(-1, 52));

        TextView register = button("Создать аккаунт");
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, 52);
        rp.setMargins(0, 10, 0, 0);
        root.addView(register, rp);

        TextView note = center("Пароль минимум 6 символов.", 13, Color.parseColor("#8A94A6"));
        root.addView(note);

        login.setOnClickListener(v -> signIn(email.getText().toString().trim(), password.getText().toString()));
        register.setOnClickListener(v -> register(email.getText().toString().trim(), password.getText().toString()));
    }

    private boolean validate(String email, String password) {
        if (!Pattern.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", email)) {
            toast("Введите корректный email");
            return false;
        }
        if (password.length() < 6) {
            toast("Пароль должен быть не короче 6 символов");
            return false;
        }
        return true;
    }

    private void signIn(String email, String password) {
        if (!validate(email, password)) return;
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && task.getResult().getUser() != null) openApp();
            else toast("Не удалось войти: " + error(task.getException()));
        });
    }

    private void register(String email, String password) {
        if (!validate(email, password)) return;
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && task.getResult().getUser() != null) openApp();
            else toast("Не удалось создать аккаунт: " + error(task.getException()));
        });
    }

    private String error(Exception e) {
        if (e == null || e.getMessage() == null) return "неизвестная ошибка";
        return e.getMessage();
    }

    private void openApp() {
        startActivity(new android.content.Intent(this, MainActivity.class));
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
