package com.montakte.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Контакте");
        title.setTextSize(32);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("MO • v0.1");
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);

        root.addView(title);
        root.addView(subtitle);
        setContentView(root);
    }
}
