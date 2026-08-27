package de.robv.android.xposed.callbacks;

import de.robv.android.xposed.IXposedMod;

public class XC_LoadPackage {
    public static class LoadPackageParam {
        public String packageName;
        public ClassLoader classLoader;
        public android.content.pm.ApplicationInfo appInfo;
    }
}
