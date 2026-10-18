package com.redmagic.fix;

import android.content.Intent;
import android.service.quicksettings.TileService;

/** 快捷设置磁贴：即使隐藏了桌面图标，也能从这里打开设置页 */
public class SettingsTileService extends TileService {
    @Override
    public void onClick() {
        super.onClick();
        final Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        unlockAndRun(new Runnable() {
            @Override
            public void run() {
                try {
                    startActivity(intent);
                } catch (Throwable ignored) {
                }
            }
        });
    }
}
