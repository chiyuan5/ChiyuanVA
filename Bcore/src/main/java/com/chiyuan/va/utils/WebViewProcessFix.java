package com.chiyuan.va.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.WebView;

import java.io.File;

public final class WebViewProcessFix {
    private static final String TAG = "WebViewProcessFix";
    private static volatile boolean sInstalled;
    private static volatile String sLastSuffix;

    private WebViewProcessFix() {
    }

    public static synchronized void install(Context context,
                                            ApplicationInfo appInfo,
                                            int userId,
                                            String packageName,
                                            String processName) {
        if (appInfo == null) {
            Slog.w(TAG, "install skipped: appInfo is null");
            return;
        }
        if (TextUtils.isEmpty(packageName)) {
            packageName = appInfo.packageName;
        }
        if (TextUtils.isEmpty(packageName)) {
            Slog.w(TAG, "install skipped: packageName is empty");
            return;
        }
        if (TextUtils.isEmpty(processName)) {
            processName = packageName;
        }

        try {
            String safeProcess = processName.replace(':', '_').replace('/', '_');
            String suffix = "u" + userId + "_" + packageName + "_" + safeProcess;
            String baseDir = appInfo.dataDir + File.separator + "app_webview_fix" + File.separator + suffix;
            String cacheDir = baseDir + File.separator + "cache";
            String cookiesDir = baseDir + File.separator + "cookies";
            String dbDir = baseDir + File.separator + "databases";

            mkdirs(baseDir);
            mkdirs(cacheDir);
            mkdirs(cookiesDir);
            mkdirs(dbDir);

            System.setProperty("webview.data.dir", baseDir);
            System.setProperty("webview.cache.dir", cacheDir);
            System.setProperty("webview.cookies.dir", cookiesDir);
            System.setProperty("webview.database.path", dbDir);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    WebView.setDataDirectorySuffix(suffix);
                    sLastSuffix = suffix;
                    Slog.d(TAG, "setDataDirectorySuffix=" + suffix);
                } catch (Throwable e) {
                    Slog.w(TAG, "setDataDirectorySuffix skipped: " + e.getMessage());
                }
            }

            // Warm up CookieManager after data dir is fixed, so WebView provider uses stable process storage.
            try {
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                Slog.d(TAG, "CookieManager initialized for process=" + processName);
            } catch (Throwable e) {
                Slog.w(TAG, "CookieManager init failed: " + e.getMessage());
            }

            sInstalled = true;
            Slog.d(TAG, "Installed stable WebView environment. pkg=" + packageName
                    + " process=" + processName + " dir=" + baseDir);
        } catch (Throwable e) {
            Slog.e(TAG, "install failed: " + e.getMessage(), e);
        }
    }

    public static boolean isInstalled() {
        return sInstalled;
    }

    public static String getLastSuffix() {
        return sLastSuffix;
    }

    private static void mkdirs(String path) {
        try {
            File dir = new File(path);
            if (!dir.exists() && !dir.mkdirs()) {
                Slog.w(TAG, "mkdirs failed: " + path);
            }
        } catch (Throwable e) {
            Slog.w(TAG, "mkdirs exception for " + path + ": " + e.getMessage());
        }
    }
}
