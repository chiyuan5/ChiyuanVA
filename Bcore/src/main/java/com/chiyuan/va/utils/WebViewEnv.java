package com.chiyuan.va.utils;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.chiyuan.va.core.env.BEnvironment;

public final class WebViewEnv {
    private static final String TAG = "WebViewEnv";
    private static final AtomicBoolean sPrepared = new AtomicBoolean(false);

    private WebViewEnv() {
    }

    public static void prepare(ApplicationInfo appInfo, String packageName, String processName, int userId) {
        if (appInfo == null || TextUtils.isEmpty(packageName)) {
            return;
        }
        ensureGuestWebViewDirs(packageName, userId);
        if (!sPrepared.compareAndSet(false, true)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String suffix = buildStableSuffix(packageName, processName, userId);
            try {
                WebView.setDataDirectorySuffix(suffix);
                Slog.d(TAG, "Applied WebView data directory suffix: " + suffix);
            } catch (Throwable e) {
                Slog.w(TAG, "Failed to apply WebView data directory suffix: " + e.getMessage());
            }
        }
    }

    public static File getGuestWebViewRoot(String packageName, int userId) {
        return new File(BEnvironment.getDataDir(packageName, userId), "app_webview");
    }

    public static File getGuestWebViewCacheRoot(String packageName, int userId) {
        return new File(BEnvironment.getDataCacheDir(packageName, userId), "WebView");
    }

    public static File getGuestWebViewDatabaseRoot(String packageName, int userId) {
        return BEnvironment.getDataDatabasesDir(packageName, userId);
    }

    public static File getGuestWebViewPrefsRoot(String packageName, int userId) {
        return new File(BEnvironment.getDataDir(packageName, userId), "shared_prefs");
    }

    public static void ensureGuestWebViewDirs(String packageName, int userId) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        FileUtils.mkdirs(getGuestWebViewRoot(packageName, userId));
        FileUtils.mkdirs(getGuestWebViewCacheRoot(packageName, userId));
        FileUtils.mkdirs(getGuestWebViewDatabaseRoot(packageName, userId));
        FileUtils.mkdirs(getGuestWebViewPrefsRoot(packageName, userId));
    }

    public static String buildStableSuffix(String packageName, String processName, int userId) {
        String processToken = TextUtils.isEmpty(processName)
                ? "main"
                : Integer.toHexString(processName.hashCode());
        String raw = "u" + userId + "_" + packageName + "_" + processToken;
        return sanitize(raw);
    }

    private static String sanitize(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "u0_default";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '.' || c == '_' || c == '-') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }
}
