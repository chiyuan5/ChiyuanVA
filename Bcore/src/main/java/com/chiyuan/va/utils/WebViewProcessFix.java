package com.chiyuan.va.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
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

            // Important: do NOT call WebView.setDataDirectorySuffix() or CookieManager.getInstance() here.
            // Some target apps call setDataDirectorySuffix() inside Application.onCreate(), and if Bcore
            // initializes WebView first, the target app will crash with:
            // "Can't set data directory suffix: WebView already initialized".
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sLastSuffix = suffix;
                Slog.d(TAG, "prepared WebView suffix=" + suffix + " (deferred, not applied by Bcore)");
            }

            sInstalled = true;
            Slog.d(TAG, "Prepared stable WebView environment without initializing provider. pkg=" + packageName
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
