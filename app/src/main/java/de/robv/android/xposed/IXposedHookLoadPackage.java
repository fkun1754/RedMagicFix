package de.robv.android.xposed;

public interface IXposedHookLoadPackage extends IXposedMod {
    void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
}
