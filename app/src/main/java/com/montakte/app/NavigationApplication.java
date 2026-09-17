package com.montakte.app;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NavigationApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, android.os.Bundle state) {
                if (!(activity instanceof MainActivity)) return;
                if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    activity.getWindow().getDecorView().postDelayed(() -> activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 910), 500);
                }
                activity.getWindow().getDecorView().postDelayed(() -> installTabs(activity), 250);
            }
            private void installTabs(Activity activity) {
                View content = activity.findViewById(android.R.id.content);
                if (!(content instanceof android.view.ViewGroup)) return;
                android.view.ViewGroup vg = (android.view.ViewGroup) content;
                if (vg.getChildCount() == 0) return;
                View rootView = vg.getChildAt(0);
                if (!(rootView instanceof LinearLayout)) return;
                LinearLayout root = (LinearLayout) rootView;
                for (int i = 0; i < root.getChildCount(); i++) {
                    View v = root.getChildAt(i);
                    if (v instanceof TextView && "💬 Сообщения".contentEquals(((TextView)v).getText())) {
                        v.setVisibility(View.GONE);
                        break;
                    }
                }
                for (int i = 0; i < root.getChildCount(); i++) {
                    View v = root.getChildAt(i);
                    if (v.getTag() instanceof String && "montakte_tabs".equals(v.getTag())) return;
                }
                LinearLayout tabs = new LinearLayout(activity);
                tabs.setTag("montakte_tabs");
                tabs.setOrientation(LinearLayout.HORIZONTAL);
                tabs.setGravity(Gravity.CENTER);
                Button messages = tab(activity, "💬\nСообщения");
                Button groups = tab(activity, "👥\nГруппы");
                Button channels = tab(activity, "📢\nКаналы");
                tabs.addView(messages, new LinearLayout.LayoutParams(0, -2, 1));
                tabs.addView(groups, new LinearLayout.LayoutParams(0, -2, 1));
                tabs.addView(channels, new LinearLayout.LayoutParams(0, -2, 1));
                root.addView(tabs, Math.min(1, root.getChildCount()));
                messages.setOnClickListener(v -> clickExistingMessages(root));
                groups.setOnClickListener(v -> {
                    Intent i = new Intent(activity, CommunitiesActivity.class);
                    i.putExtra("communityType", "group");
                    activity.startActivity(i);
                });
                channels.setOnClickListener(v -> {
                    Intent i = new Intent(activity, CommunitiesActivity.class);
                    i.putExtra("communityType", "channel");
                    activity.startActivity(i);
                });
            }
            private Button tab(Activity a, String s) {
                Button b = new Button(a);
                b.setText(s);
                b.setTextSize(13);
                b.setTextColor(Color.BLACK);
                return b;
            }
            private void clickExistingMessages(LinearLayout root) {
                for (int i = 0; i < root.getChildCount(); i++) {
                    View v = root.getChildAt(i);
                    if (v instanceof Button && "💬 Сообщения".contentEquals(((Button)v).getText())) {
                        v.performClick();
                        return;
                    }
                }
            }
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityResumed(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, android.os.Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }
}
