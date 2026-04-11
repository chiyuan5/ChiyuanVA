package com.chiyuan.va.utils;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.chiyuan.va.core.env.BEnvironment;

public final class WebViewEnv {
    private static final String TAG = "WebViewEnv";
    private static final AtomicBoolean sPrepared = new AtomicBoolean(false);
    private static final AtomicBoolean sLegacyStateCleaned = new AtomicBoolean(false);

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
        if (shouldUseDataDirectorySuffix() && sLegacyStateCleaned.compareAndSet(false, true)) {
            // 不再在每次 guest 进程启动时主动清空 WebView 旧状态。
            // 之前这样做虽然能规避目录冲突，但会把 cookies / prefs / 历史状态一起打掉，
            // 某些 SDK 的协议页/隐私页会因此反复重试并伴随 ERR_CACHE_MISS。
            Slog.d(TAG, "Skip aggressive legacy WebView cleanup for " + packageName + " on Android P+");
        }
        if (shouldUseDataDirectorySuffix()) {
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

    public static boolean shouldUseDataDirectorySuffix() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    public static void clearLegacyWebViewState(String packageName, int userId) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }

        int deletedEntries = 0;
        deletedEntries += FileUtils.deleteDir(getGuestWebViewRoot(packageName, userId));
        deletedEntries += FileUtils.deleteDir(getGuestWebViewCacheRoot(packageName, userId));

        File dbRoot = getGuestWebViewDatabaseRoot(packageName, userId);
        deletedEntries += deleteIfExists(new File(dbRoot, "webview.db"));
        deletedEntries += deleteIfExists(new File(dbRoot, "webviewCache.db"));
        deletedEntries += deleteIfExists(new File(dbRoot, "webviewCookiesChromium.db"));

        File prefsRoot = getGuestWebViewPrefsRoot(packageName, userId);
        deletedEntries += deleteIfExists(new File(prefsRoot, "WebViewChromiumPrefs.xml"));
        deletedEntries += deleteIfExists(new File(prefsRoot, "webview_preferences.xml"));

        ensureGuestWebViewDirs(packageName, userId);
        Slog.d(TAG, String.format(Locale.US,
                "Cleared %d legacy WebView state entries for %s on Android P+", deletedEntries, packageName));
    }

    public static String buildStableSuffix(String packageName, String processName, int userId) {
        String processToken = TextUtils.isEmpty(processName)
                ? "main"
                : Integer.toHexString(processName.hashCode());
        String raw = "u" + userId + "_" + packageName + "_" + processToken;
        return sanitize(raw);
    }

    private static int deleteIfExists(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (file.isDirectory()) {
            return FileUtils.deleteDir(file);
        }
        return file.delete() ? 1 : 0;
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
