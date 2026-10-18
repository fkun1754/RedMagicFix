package com.redmagic.fix;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/** 只读配置提供方：Hook 在各进程里通过它读取开关，不需要 root 也不需要系统设置权限 */
public class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY = "com.redmagic.fix.config";
    public static final String PREFS = "config";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String key = uri.getLastPathSegment();
        SharedPreferences sp = getContext().getSharedPreferences(PREFS, 0);
        boolean value = sp.getBoolean(key, Prefs.defaultFor(key));
        MatrixCursor cursor = new MatrixCursor(new String[]{"value"});
        cursor.addRow(new Object[]{value ? 1 : 0});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }
}
