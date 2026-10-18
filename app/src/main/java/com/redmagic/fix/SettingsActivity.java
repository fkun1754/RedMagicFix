package com.redmagic.fix;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private static final String[] KEYS = {
            Prefs.SPLIT, Prefs.SMALL_WINDOW, Prefs.DOUBLE_APP, Prefs.GAME_KEY
    };
    private static final String[] TITLES = {
            "分屏比例无级调节", "小窗不限应用", "应用分身不限应用", "手柄 Home 键放行"
    };
    private static final String[] DESCS = {
            "分隔条拖到任意位置松手即停",
            "常规小窗与游戏模式小窗可打开任意第三方 App",
            "分身列表显示所有已安装的第三方 App",
            "游戏里 Home 键不再唤出游戏助手，完整交给游戏；侧滑面板保持可用"
    };
    private static final boolean[] DEFAULTS = {true, true, true, false};

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
        root.addView(buildRow(3));

        addSection(root, "其他");
        root.addView(buildHideIconRow());

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
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences(ConfigProvider.PREFS, 0).edit()
                    .putBoolean(KEYS[index], isChecked).apply();
        });
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

    private LinearLayout buildHideIconRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        Switch sw = new Switch(this);
        sw.setText("显示桌面图标");
        sw.setTextSize(16);
        sw.setTextColor(Color.BLACK);
        sw.setChecked(!isIconHidden());
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setIconHidden(!isChecked);
            if (!isChecked) {
                Toast.makeText(this,
                        "已隐藏桌面图标：可从 LSPosed 的模块设置入口打开",
                        Toast.LENGTH_LONG).show();
            }
        });
        row.addView(sw);

        TextView desc = new TextView(this);
        desc.setText("关闭后桌面不再显示图标，可从 LSPosed → 模块 → RedMagic Fix 的设置入口打开；重新打开开关即可恢复");
        desc.setTextSize(13);
        desc.setTextColor(Color.GRAY);
        desc.setPadding(dp(4), dp(2), 0, 0);
        row.addView(desc);
        return row;
    }

    private boolean isIconHidden() {
        ComponentName alias = new ComponentName(this, getPackageName() + ".Launcher");
        return getPackageManager().getComponentEnabledSetting(alias)
                != PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private void setIconHidden(boolean hidden) {
        try {
            ComponentName alias = new ComponentName(this, getPackageName() + ".Launcher");
            int state = hidden
                    ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            getPackageManager().setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
        } catch (Throwable t) {
            Toast.makeText(this, "切换桌面图标失败", Toast.LENGTH_SHORT).show();
        }
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
        return getSharedPreferences(ConfigProvider.PREFS, 0).getBoolean(key, def);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
