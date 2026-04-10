package com.chiyuan.va.fake.service;

import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import java.io.File;
import java.lang.reflect.Method;

import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.app.BActivityThread;
import com.chiyuan.va.fake.hook.IInjectHook;
import com.chiyuan.va.utils.Slog;


public class WebViewProxy implements IInjectHook {
    public static final String TAG = "WebViewProxy";

    private static volatile boolean sSuffixSet = false;

    @Override
    public void injectHook() {
        if (sSuffixSet) {
            Slog.d(TAG, "WebView data directory suffix already set, skipping");
            return;
        }

        try {
            int userId = BActivityThread.getUserId();
            int pid = android.os.Process.myPid();
            String suffix = "chiyuan_" + userId + "_" + pid;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+: use the official API to isolate WebView data per virtual user/process
                WebView.setDataDirectorySuffix(suffix);
                sSuffixSet = true;
                Slog.d(TAG, "Set WebView data directory suffix: " + suffix);
            } else {
                // API < 28: manually create isolated directories and set system properties
                setDataDirectoryLegacy(suffix);
                sSuffixSet = true;
            }
        } catch (IllegalStateException e) {
            // setDataDirectorySuffix throws if a WebView has already been created in this process.
            // This can happen if another hook or library creates a WebView before us.
            Slog.w(TAG, "WebView already initialized in this process, cannot set suffix: " + e.getMessage());
            sSuffixSet = true; // mark as done to avoid repeated attempts
        } catch (Exception e) {
            Slog.e(TAG, "Failed to set WebView data directory suffix", e);
        }
    }

    /**
     * For API < 28, set data directory via system properties and ensure the directory exists.
     */
    private void setDataDirectoryLegacy(String suffix) {
        try {
            Context context = ChiyuanVACore.getContext();
            if (context == null) {
                Slog.w(TAG, "Context is null, cannot set legacy WebView data dir");
                return;
            }

            String baseDir = context.getApplicationInfo().dataDir;
            String uniqueDataDir = baseDir + "/webview_" + suffix;

            File dataDir = new File(uniqueDataDir);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // These system properties are checked by some older WebView implementations
            System.setProperty("webview.data.dir", uniqueDataDir);
            System.setProperty("webview.cache.dir", uniqueDataDir + "/cache");

            // Also try the reflection approach for WebViewFactory on older APIs
            try {
                Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");
                Method setDataDirectorySuffix = webViewFactoryClass.getDeclaredMethod(
                        "setDataDirectorySuffix", String.class);
                setDataDirectorySuffix.setAccessible(true);
                setDataDirectorySuffix.invoke(null, suffix);
                Slog.d(TAG, "Set WebView suffix via WebViewFactory reflection: " + suffix);
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory reflection fallback failed: " + e.getMessage());
            }

            Slog.d(TAG, "Legacy WebView data dir set: " + uniqueDataDir);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to set legacy WebView data directory", e);
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
