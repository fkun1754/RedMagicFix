package com.redmagic.splitfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * 红魔 8S Pro+ 分屏比例无级调节模块
 *
 * 现象：横屏左右分屏时分隔条只能停在 1:1（AOSP 的 16:9 停靠点在
 * 20:9 超长屏横屏时计算越界，只剩 middle 一个停靠点）。
 *
 * 修复：hook DividerSnapAlgorithm.calculateSnapTarget，
 *   分隔条在屏幕内任意位置松手 → 自由停靠（无级调节，横竖屏都生效）；
 *   拖出屏幕边缘 → 走系统原逻辑（关闭分屏的官方功能保留）。
 */
public class FixSplitRatio implements IXposedHookLoadPackage {

    private static final String TAG = "FixSplitRatio";
    private static final String ALGO_CLASS = "com.android.wm.shell.common.split.DividerSnapAlgorithm";
    private static final String TARGET_CLASS = "com.android.wm.shell.common.split.DividerSnapAlgorithm$SnapTarget";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }
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

                            // 屏幕内（两个 dismiss 边界之间）→ 无级自由停靠
                            Object dismissStart = XposedHelpers.callMethod(algo, "getDismissStartTarget");
                            Object dismissEnd = XposedHelpers.callMethod(algo, "getDismissEndTarget");
                            int start = XposedHelpers.getIntField(dismissStart, "position");
                            int end = XposedHelpers.getIntField(dismissEnd, "position");
                            if (pos > start && pos < end) {
                                // snapPosition=10 是 AOSP 的自由停靠类型，snapToTarget 会停在当前位置
                                param.setResult(XposedHelpers.newInstance(targetClass, pos, pos, 10));
                            }
                            // 否则（拖出屏幕边缘）不拦截 → 系统原逻辑关闭分屏
                        }
                    });
            Log.d(TAG, "hook installed: 无级分屏比例 (dismiss 保留)");
        } catch (Throwable t) {
            Log.e(TAG, "hook init failed", t);
        }
    }
}
