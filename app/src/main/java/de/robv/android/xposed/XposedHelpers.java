package de.robv.android.xposed;

import java.lang.reflect.Method;

public class XposedHelpers {
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try { return Class.forName(className, false, classLoader); }
        catch (ClassNotFoundException e) { throw new RuntimeException(e); }
    }

    public static Object getObjectField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        return (Boolean) getObjectField(obj, fieldName);
    }

    public static int getIntField(Object obj, String fieldName) {
        return (Integer) getObjectField(obj, fieldName);
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static Object newInstance(Class<?> clazz, Object... params) {
        Class<?>[] types = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) types[i] = params[i].getClass();
        try {
            java.lang.reflect.Constructor<?> c = clazz.getDeclaredConstructor(types);
            c.setAccessible(true);
            return c.newInstance(params);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        try {
            Method m = obj.getClass().getMethod(methodName, types);
            m.setAccessible(true);
            return m.invoke(obj, args);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName,
            Object... parameterTypesAndCallback) {
        int n = parameterTypesAndCallback.length;
        Object callback = parameterTypesAndCallback[n - 1];
        Class<?>[] types = new Class<?>[n - 1];
        for (int i = 0; i < n - 1; i++) {
            Object t = parameterTypesAndCallback[i];
            types[i] = (t instanceof Class) ? (Class<?>) t : t.getClass();
        }
        try {
            Method m = clazz.getDeclaredMethod(methodName, types);
            m.setAccessible(true);
            return new XC_MethodHook.Unhook(m, (XC_MethodHook) callback);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
