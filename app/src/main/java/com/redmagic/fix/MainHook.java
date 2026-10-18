package com.redmagic.fix;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * RedMagic Fix —— 红魔 8S Pro+ 限制解除
 *
 * 1) 分屏比例无级调节（com.android.systemui）
 * 2) 小窗支持判断放行（android / system_server，另在各小窗 App 内兜底）
 * 3) 应用分身支持列表放开（com.zte.cn.doubleapp）
 * 4) 手柄震动转手机震动（头玩）
 * 5) 红魔键 BTN_MODE 放行（android / system_server）
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RedMagicFix";

    private static final String ALGO_CLASS = "com.android.wm.shell.common.split.DividerSnapAlgorithm";
    private static final String TARGET_CLASS = "com.android.wm.shell.common.split.DividerSnapAlgorithm$SnapTarget";

    private static final String SYSTEMUI_PKG = "com.android.systemui";
    private static final String FRAMEWORK_PKG = "android";
    private static final String DOUBLEAPP_PKG = "com.zte.cn.doubleapp";

    /** 小窗相关 App：框架 hook 不生效时在这些进程里兜底 */
    private static final String[] SMALL_WIN_APP_PKGS = {
            "com.zte.floatassist",       // 常规悬浮小窗
            "cn.zte.gamefloat",          // 游戏浮窗服务
            "cn.nubia.gamelauncher",     // 游戏空间
            "cn.nubia.gameassist"        // 游戏助手
    };

    /** 需要把"手柄震动"转成"手机震动"的应用 */
    private static final String[] VIBRATE_APP_PKGS = {
            "com.headup.epic.smbf"       // 头玩
    };

    private static final String[] CFG_KEYS = {
            Prefs.SPLIT, Prefs.SMALL_WINDOW, Prefs.DOUBLE_APP, Prefs.VIBRATOR, Prefs.GAME_KEY
    };
    private static final boolean[] CFG_DEFAULTS = {true, true, true, true, false};
    private static final boolean[] cfgValues = CFG_DEFAULTS.clone();
    private static long cfgLastRead = 0;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;
        if (SYSTEMUI_PKG.equals(pkg)) {
            hookSplitRatio(lpparam);
        } else if (FRAMEWORK_PKG.equals(pkg)) {
            hookSmallWindow(lpparam);
        } else if (DOUBLEAPP_PKG.equals(pkg)) {
            hookDoubleApp(lpparam);
        }

        for (String target : SMALL_WIN_APP_PKGS) {
            if (target.equals(pkg)) {
                hookSmallWindowInApp(lpparam);
                break;
            }
        }

        for (String target : VIBRATE_APP_PKGS) {
            if (target.equals(pkg)) {
                hookGamepadVibrator(lpparam);
                break;
            }
        }
    }

    /** 分隔条自由停靠：屏幕内任意位置松手即停，拖出边缘仍走系统逻辑关闭分屏 */
    private void hookSplitRatio(LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;
        try {
            final Class<?> algoClass = XposedHelpers.findClass(ALGO_CLASS, cl);
            final Class<?> targetClass = XposedHelpers.findClass(TARGET_CLASS, cl);

            XposedHelpers.findAndHookMethod(algoClass, "calculateSnapTarget",
                    int.class, float.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled(Prefs.SPLIT)) {
                                return;
                            }
                            Object algo = param.thisObject;
                            int pos = (int) param.args[0];

                            Object dismissStart = XposedHelpers.callMethod(algo, "getDismissStartTarget");
                            Object dismissEnd = XposedHelpers.callMethod(algo, "getDismissEndTarget");
                            int start = XposedHelpers.getIntField(dismissStart, "position");
                            int end = XposedHelpers.getIntField(dismissEnd, "position");
                            if (pos > start && pos < end) {
                                param.setResult(XposedHelpers.newInstance(targetClass, pos, pos, 10));
                            }
                        }
                    });
            Log.d(TAG, "hook installed: 无级分屏比例");
        } catch (Throwable t) {
            Log.e(TAG, "split ratio hook failed", t);
        }
    }

    /** 小窗支持判断恒为 true：常规小窗 + 游戏模式小窗共用这一处判断 */
    private void hookSmallWindow(LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;
        try {
            Class<?> acc = XposedHelpers.findClass(
                    "com.android.server.wm.ActivityClientController", cl);
            XposedHelpers.findAndHookMethod(acc, "checkTaskSupportWr", int.class,
                    alwaysTrue(Prefs.SMALL_WINDOW));
            Log.d(TAG, "hook installed: ActivityClientController.checkTaskSupportWr");
        } catch (Throwable t) {
            Log.e(TAG, "checkTaskSupportWr hook failed", t);
        }

        try {
            Class<?> tda = XposedHelpers.findClass(
                    "com.android.server.wm.TaskDisplayAreaMifavor", cl);
            XposedHelpers.findAndHookMethod(tda, "checkTaskSupportForCompWr",
                    Context.class, ComponentName.class, alwaysTrue(Prefs.SMALL_WINDOW));
            Log.d(TAG, "hook installed: checkTaskSupportForCompWr");

            // 真正切 freeform 前的第二道校验，不通过会弹"该应用不支持小窗"
            Class<?> record = XposedHelpers.findClass("com.android.server.wm.ActivityRecord", cl);
            Class<?> task = XposedHelpers.findClass("com.android.server.wm.Task", cl);
            hookReturn(Prefs.SMALL_WINDOW, tda, "isSupportWindowReplyState", 0, record, Context.class);
            hookReturn(Prefs.SMALL_WINDOW, tda, "isSupportWindowReplyStateForTask", 0, task, Context.class);
            hookReturn(Prefs.SMALL_WINDOW, tda, "isSupportWindowReply", true, int.class);
            hookReturn(Prefs.SMALL_WINDOW, tda, "isResizeable", true, record);
            Log.d(TAG, "hook installed: WindowReply state checks");
        } catch (Throwable t) {
            Log.e(TAG, "checkTaskSupportForCompWr hook failed", t);
        }

        // 红魔键：ZTE 在游戏场景里把 BTN_MODE 拦下并让游戏助手弹面板，
        // 这里把该键交还给正常的窗口策略链路（侧滑唤出不受影响）
        try {
            Class<?> ims = XposedHelpers.findClass(
                    "com.android.server.input.InputManagerService", cl);
            XposedHelpers.findAndHookMethod(ims, "interceptKeyBeforeQueueing",
                    KeyEvent.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!enabled(Prefs.GAME_KEY)) {
                                return;
                            }
                            KeyEvent event = (KeyEvent) param.args[0];
                            if (event.getKeyCode() != KeyEvent.KEYCODE_BUTTON_MODE) {
                                return;
                            }
                            try {
                                Object callbacks = XposedHelpers.getObjectField(
                                        param.thisObject, "mWindowManagerCallbacks");
                                Object result = XposedHelpers.callMethod(callbacks,
                                        "interceptKeyBeforeQueueing",
                                        event, (Integer) param.args[1]);
                                param.setResult(result);
                            } catch (Throwable t) {
                                Log.e(TAG, "pass BTN_MODE to policy failed", t);
                            }
                        }
                    });
            Log.d(TAG, "hook installed: BTN_MODE pass-through");
        } catch (Throwable t) {
            Log.e(TAG, "BTN_MODE hook failed", t);
        }
    }

    /** App 内兜底：floatassist 的 Utils 和公共 ActivityManagerWrapper */
    private void hookSmallWindowInApp(LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;
        try {
            Class<?> utils = XposedHelpers.findClass("com.zte.floatassist.util.Utils", cl);
            XposedHelpers.findAndHookMethod(utils, "checkTaskSupportWr",
                    Context.class, String.class, alwaysTrue(Prefs.SMALL_WINDOW));
            Log.d(TAG, "hook installed: Utils.checkTaskSupportWr");
        } catch (Throwable t) {
            Log.d(TAG, "Utils.checkTaskSupportWr not present: " + t);
        }

        try {
            Class<?> wrapper = XposedHelpers.findClass(
                    "com.zte.shared.wrapper.ActivityManagerWrapper", cl);
            XposedHelpers.findAndHookMethod(wrapper, "checkTaskSupportWr", String.class,
                    alwaysTrue(Prefs.SMALL_WINDOW));
            XposedHelpers.findAndHookMethod(wrapper, "checkTaskSupportWr", ComponentName.class,
                    alwaysTrue(Prefs.SMALL_WINDOW));
            XposedHelpers.findAndHookMethod(wrapper, "checkTaskSupportWr", int.class,
                    alwaysTrue(Prefs.SMALL_WINDOW));
            Log.d(TAG, "hook installed: ActivityManagerWrapper.checkTaskSupportWr");
        } catch (Throwable t) {
            Log.d(TAG, "ActivityManagerWrapper.checkTaskSupportWr not present: " + t);
        }
    }

    private static XC_MethodHook alwaysTrue(final String cfgKey) {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (enabled(cfgKey)) {
                    param.setResult(Boolean.TRUE);
                }
            }
        };
    }

    /**
     * 手柄没有马达：把 InputDevice.getVibrator() 的 null 换成手机自己的振动器，
     * 游戏调用震动时直接震手机。
     */
    private void hookGamepadVibrator(LoadPackageParam lpparam) {
        try {
            Class<?> inputDevice = XposedHelpers.findClass("android.view.InputDevice", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(inputDevice, "getVibrator", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!enabled(Prefs.VIBRATOR)) {
                        return;
                    }
                    Object original = param.getResult();
                    if (original != null) {
                        return;
                    }
                    try {
                        Context ctx = context();
                        if (ctx == null) {
                            return;
                        }
                        Object manager = ctx.getSystemService("vibrator_manager");
                        if (manager == null) {
                            return;
                        }
                        Object vibrator = XposedHelpers.callMethod(manager, "getDefaultVibrator");
                        if (vibrator != null) {
                            param.setResult(vibrator);
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "provide phone vibrator failed", t);
                    }
                }
            });
            Log.d(TAG, "hook installed: InputDevice.getVibrator -> phone vibrator");
        } catch (Throwable t) {
            Log.e(TAG, "vibrator hook failed", t);
        }
    }

    /** 分身支持列表：返回所有带桌面图标的第三方 App */
    private void hookDoubleApp(LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled(Prefs.DOUBLE_APP)) {
                    return;
                }
                try {
                    Context ctx = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (ctx == null) {
                        return;
                    }
                    List<String> all = collectLaunchableApps(ctx);
                    if (all != null && !all.isEmpty()) {
                        param.setResult(all);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "collect launchable apps failed", t);
                }
            }
        };

        Class<?> cls;
        try {
            cls = XposedHelpers.findClass("com.zte.cn.doubleapp.common.UpdateUtils", cl);
        } catch (Throwable t) {
            Log.e(TAG, "UpdateUtils not found", t);
            return;
        }
        for (String method : new String[]{"getSupportApps", "getSupportAppsLocal", "getSupportAppsOld"}) {
            try {
                XposedHelpers.findAndHookMethod(cls, method, hook);
                Log.d(TAG, "hook installed: UpdateUtils." + method);
            } catch (Throwable t) {
                Log.e(TAG, "doubleapp hook failed: " + method, t);
            }
        }
    }

    private static List<String> collectLaunchableApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolves = pm.queryIntentActivities(intent, 0);
        LinkedHashSet<String> pkgs = new LinkedHashSet<>();
        String self = ctx.getPackageName();
        for (ResolveInfo ri : resolves) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || ai.applicationInfo == null) {
                continue;
            }
            String p = ai.packageName;
            if (p == null || p.equals(self)) {
                continue;
            }
            int flags = ai.applicationInfo.flags;
            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }
            if ((flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                continue;
            }
            pkgs.add(p);
        }
        return new ArrayList<>(pkgs);
    }

    /** 把静态方法的返回值固定成指定值（仅在开关打开时） */
    private static void hookReturn(String cfgKey, Class<?> cls, String method, final Object value,
                                   Object... paramTypes) {
        Object[] args = new Object[paramTypes.length + 1];
        System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
        args[paramTypes.length] = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (enabled(cfgKey)) {
                    param.setResult(value);
                }
            }
        };
        XposedHelpers.findAndHookMethod(cls, method, args);
    }

    private static boolean enabled(String key) {
        long now = SystemClock.elapsedRealtime();
        if (now - cfgLastRead > 1500L) {
            refreshConfig();
            cfgLastRead = now;
        }
        for (int i = 0; i < CFG_KEYS.length; i++) {
            if (CFG_KEYS[i].equals(key)) {
                return cfgValues[i];
            }
        }
        return true;
    }

    private static void refreshConfig() {
        try {
            Context ctx = context();
            if (ctx == null) {
                return;
            }
            ContentResolver cr = ctx.getContentResolver();
            for (int i = 0; i < CFG_KEYS.length; i++) {
                cfgValues[i] = Settings.System.getInt(cr, CFG_KEYS[i],
                        CFG_DEFAULTS[i] ? 1 : 0) == 1;
            }
        } catch (Throwable t) {
            Log.e(TAG, "read config failed", t);
        }
    }

    private static Context context() {
        try {
            Object app = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", null),
                    "currentApplication");
            if (app instanceof Context) {
                return (Context) app;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> at = XposedHelpers.findClass("android.app.ActivityThread", null);
            Object thread = XposedHelpers.callStaticMethod(at, "currentActivityThread");
            Object systemContext = XposedHelpers.callMethod(thread, "getSystemContext");
            if (systemContext instanceof Context) {
                return (Context) systemContext;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
