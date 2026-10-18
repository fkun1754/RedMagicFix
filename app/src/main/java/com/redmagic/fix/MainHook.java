package com.redmagic.fix;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
            XposedHelpers.findAndHookMethod(acc, "checkTaskSupportWr", int.class, alwaysTrue());
            Log.d(TAG, "hook installed: ActivityClientController.checkTaskSupportWr");
        } catch (Throwable t) {
            Log.e(TAG, "checkTaskSupportWr hook failed", t);
        }

        try {
            Class<?> tda = XposedHelpers.findClass(
                    "com.android.server.wm.TaskDisplayAreaMifavor", cl);
            XposedHelpers.findAndHookMethod(tda, "checkTaskSupportForCompWr",
                    Context.class, ComponentName.class, alwaysTrue());
            Log.d(TAG, "hook installed: checkTaskSupportForCompWr");
        } catch (Throwable t) {
            Log.e(TAG, "checkTaskSupportForCompWr hook failed", t);
        }
    }

    /** App 内兜底：floatassist 的 Utils 和公共 ActivityManagerWrapper */
    private void hookSmallWindowInApp(LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;
        try {
            Class<?> utils = XposedHelpers.findClass("com.zte.floatassist.util.Utils", cl);
            XposedHelpers.findAndHookMethod(utils, "checkTaskSupportWr",
                    Context.class, String.class, alwaysTrue());
            Log.d(TAG, "hook installed: Utils.checkTaskSupportWr");
        } catch (Throwable t) {
            Log.d(TAG, "Utils.checkTaskSupportWr not present: " + t);
        }

        try {
            Class<?> wrapper = XposedHelpers.findClass(
                    "com.zte.shared.wrapper.ActivityManagerWrapper", cl);
            XposedHelpers.findAndHookMethod(wrapper, "checkTaskSupportWr", String.class, alwaysTrue());
            XposedHelpers.findAndHookMethod(wrapper, "checkTaskSupportWr", ComponentName.class, alwaysTrue());
            XposedHelpers.findAndHookMethod(wrapper, "checkTaskSupportWr", int.class, alwaysTrue());
            Log.d(TAG, "hook installed: ActivityManagerWrapper.checkTaskSupportWr");
        } catch (Throwable t) {
            Log.d(TAG, "ActivityManagerWrapper.checkTaskSupportWr not present: " + t);
        }
    }

    private static XC_MethodHook alwaysTrue() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult(Boolean.TRUE);
            }
        };
    }

    /** 分身支持列表：返回所有带桌面图标的第三方 App */
    private void hookDoubleApp(LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
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
}
