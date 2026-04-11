package com.chiyuan.va.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.reflect.Field;

/**
 * 修复虚拟进程中 WebView 出现 net::ERR_CACHE_MISS 的问题。
 *
 * 根因：SELinux 阻止虚拟进程创建 netlink_route_socket，
 * Chromium 通过 netlink 查询网络接口状态失败后认为无网络，
 * 缓存也是空的 → ERR_CACHE_MISS。
 *
 * 修复策略：
 * 1. bindProcessToNetwork() — 让 socket 走已绑定的网络（在 BActivityThread 中调用）
 * 2. 本类提供 WebViewClient 包装 — 对遗漏的 WebView 在 onReceivedError 时
 *    切换 cacheMode 为 LOAD_NO_CACHE 并重试
 * 3. 注册网络回调保持绑定
 */
public class WebViewNetworkFix {

    private static final String TAG = "WebViewNetworkFix";
    private static volatile boolean sNetworkCallbackRegistered = false;

    /**
     * 配置一个 WebView 实例，确保网络可用并设置正确的缓存模式。
     * 在 WebView 创建后、loadUrl 前调用。
     */
    public static void configureWebView(WebView webView) {
        if (webView == null) return;
        try {
            // 强制告诉 Chromium 网络可用
            webView.setNetworkAvailable(true);

            WebSettings settings = webView.getSettings();
            if (settings != null) {
                // LOAD_NO_CACHE: 完全不依赖缓存，每次从网络加载
                // 这是解决 ERR_CACHE_MISS 最直接的方式
                settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                settings.setDomStorageEnabled(true);
                settings.setDatabaseEnabled(true);
                settings.setBlockNetworkLoads(false);
                settings.setBlockNetworkImage(false);
                settings.setJavaScriptEnabled(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                }
            }
        } catch (Throwable t) {
            Slog.w(TAG, "configureWebView failed", t);
        }
    }

    /**
     * 包装 WebView 的 WebViewClient，拦截 ERR_CACHE_MISS 错误并自动重试。
     * 适用于无法修改第三方 SDK 中 WebView 配置的场景。
     */
    public static void wrapWebViewClient(final WebView webView) {
        if (webView == null) return;
        try {
            // 获取当前的 WebViewClient
            WebViewClient originalClient = getWebViewClient(webView);

            webView.setWebViewClient(new WebViewClient() {
                private boolean mRetried = false;

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    // errorCode -1 = ERROR_UNKNOWN, description 含 ERR_CACHE_MISS
                    if (!mRetried && description != null && description.contains("ERR_CACHE_MISS")) {
                        mRetried = true;
                        Slog.d(TAG, "ERR_CACHE_MISS detected, retrying with LOAD_NO_CACHE: " + failingUrl);

                        WebSettings settings = view.getSettings();
                        if (settings != null) {
                            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                        }
                        view.setNetworkAvailable(true);

                        // 重新加载
                        if (failingUrl != null && !failingUrl.startsWith("about:") && !failingUrl.startsWith("queue_message")) {
                            view.loadUrl(failingUrl);
                            return;
                        }
                    }
                    if (originalClient != null) {
                        originalClient.onReceivedError(view, errorCode, description, failingUrl);
                    } else {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (originalClient != null) {
                        return originalClient.shouldOverrideUrlLoading(view, url);
                    }
                    return super.shouldOverrideUrlLoading(view, url);
                }

                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    if (originalClient != null) {
                        originalClient.onPageStarted(view, url, favicon);
                    } else {
                        super.onPageStarted(view, url, favicon);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    if (originalClient != null) {
                        originalClient.onPageFinished(view, url);
                    } else {
                        super.onPageFinished(view, url);
                    }
                }
            });
        } catch (Throwable t) {
            Slog.w(TAG, "wrapWebViewClient failed", t);
        }
    }

    /**
     * 遍历 View 树，找到所有 WebView 并配置+包装
     */
    public static void fixWebViewsInViewTree(View root) {
        if (root == null) return;
        if (root instanceof WebView) {
            WebView wv = (WebView) root;
            configureWebView(wv);
            wrapWebViewClient(wv);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                fixWebViewsInViewTree(group.getChildAt(i));
            }
        }
    }

    /**
     * 注册持久网络回调，当网络变化时自动重新绑定进程到可用网络。
     * 在 BActivityThread.handleBindApplication() 中调用一次即可。
     */
    public static void registerNetworkCallback(Context context) {
        if (sNetworkCallbackRegistered) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Slog.d(TAG, "Network available, binding process: " + network);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cm.bindProcessToNetwork(network);
                        } else {
                            ConnectivityManager.setProcessDefaultNetwork(network);
                        }
                    } catch (Throwable t) {
                        Slog.w(TAG, "bindProcessToNetwork failed", t);
                    }
                }
            });

            sNetworkCallbackRegistered = true;
            Slog.d(TAG, "Network callback registered");
        } catch (Throwable t) {
            Slog.w(TAG, "registerNetworkCallback failed", t);
        }
    }

    /**
     * 通过反射获取 WebView 当前的 WebViewClient
     */
    private static WebViewClient getWebViewClient(WebView webView) {
        try {
            // WebView 内部通过 mProvider 持有 WebViewClient
            Field providerField = WebView.class.getDeclaredField("mProvider");
            providerField.setAccessible(true);
            Object provider = providerField.get(webView);
            if (provider != null) {
                // WebViewProvider 实现类中通常有 mContentsClientAdapter 或 mWebViewClient
                for (Field f : provider.getClass().getDeclaredFields()) {
                    if (WebViewClient.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        return (WebViewClient) f.get(provider);
                    }
                }
                // 尝试深入一层
                for (Field f : provider.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object inner = f.get(provider);
                    if (inner != null) {
                        for (Field ff : inner.getClass().getDeclaredFields()) {
                            if (WebViewClient.class.isAssignableFrom(ff.getType())) {
                                ff.setAccessible(true);
                                return (WebViewClient) ff.get(inner);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // 反射失败不影响功能
        }
        return null;
    }
}
