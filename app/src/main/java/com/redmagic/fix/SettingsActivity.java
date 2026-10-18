package com.redmagic.fix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private static final String[] KEYS = {
            Prefs.SPLIT, Prefs.SMALL_WINDOW, Prefs.DOUBLE_APP, Prefs.VIBRATOR, Prefs.GAME_KEY
    };
    private static final String[] TITLES = {
            "分屏比例无级调节", "小窗不限应用", "应用分身不限应用", "手柄震动转手机震动", "手柄 Home 键放行"
    };
    private static final String[] DESCS = {
            "分隔条拖到任意位置松手即停",
            "常规小窗与游戏模式小窗可打开任意第三方 App",
            "分身列表显示所有已安装的第三方 App",
            "手柄没有马达时，游戏震动改为震动手机",
            "游戏里 Home 键不再唤出游戏助手，完整交给游戏；侧滑面板保持可用"
    };
    private static final boolean[] DEFAULTS = {true, true, true, true, false};

    private final Switch[] switches = new Switch[KEYS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("RedMagic Fix");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("修改后立即生效，无需重启（系统框架内的改动需重启手机）");
        hint.setTextSize(13);
        hint.setTextColor(Color.GRAY);
        hint.setPadding(0, dp(6), 0, dp(16));
        root.addView(hint);

        addSection(root, "手机功能修改");
        for (int i = 0; i < 3; i++) {
            root.addView(buildRow(i));
        }
        addSection(root, "手柄功能修改");
        for (int i = 3; i < KEYS.length; i++) {
            root.addView(buildRow(i));
        }

        if (!Settings.System.canWrite(this)) {
            Button grant = new Button(this);
            grant.setText("授予修改系统设置权限");
            grant.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
            root.addView(grant);
        }

        setContentView(scroll);
    }

    private LinearLayout buildRow(final int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        Switch sw = new Switch(this);
        sw.setText(TITLES[index]);
        sw.setTextSize(16);
        sw.setTextColor(Color.BLACK);
        sw.setChecked(readSetting(KEYS[index], DEFAULTS[index]));
        sw.setOnCheckedChangeListener((buttonView, isChecked) ->
                Settings.System.putInt(getContentResolver(), KEYS[index], isChecked ? 1 : 0));
        switches[index] = sw;
        row.addView(sw);

        TextView desc = new TextView(this);
        desc.setText(DESCS[index]);
        desc.setTextSize(13);
        desc.setTextColor(Color.GRAY);
        desc.setPadding(dp(4), dp(2), 0, 0);
        row.addView(desc);

        return row;
    }

    private void addSection(LinearLayout root, String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(13);
        header.setTextColor(Color.GRAY);
        header.setPadding(0, dp(18), 0, dp(2));
        root.addView(header);
    }

    private boolean readSetting(String key, boolean def) {
        return Settings.System.getInt(getContentResolver(), key, def ? 1 : 0) == 1;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
