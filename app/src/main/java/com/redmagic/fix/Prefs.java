package com.redmagic.fix;

/** 设置项键名，模块 App 与 Hook 共用（不依赖 Xposed 类） */
public final class Prefs {
    public static final String SPLIT = "redmagic_fix_split";
    public static final String SMALL_WINDOW = "redmagic_fix_smallwin";
    public static final String DOUBLE_APP = "redmagic_fix_doubleapp";
    public static final String VIBRATOR = "redmagic_fix_vibrate";
    public static final String GAME_KEY = "redmagic_fix_gamekey";
    public static final String HIDE_ICON = "redmagic_fix_hide_icon";

    private Prefs() {
    }
}
