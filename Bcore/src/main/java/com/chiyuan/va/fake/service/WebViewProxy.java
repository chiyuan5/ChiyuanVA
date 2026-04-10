package com.chiyuan.va.fake.service;

import android.content.Context;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;
import java.lang.reflect.Method;

import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.app.BActivityThread;
import com.chiyuan.va.fake.hook.ClassInvocationStub;
import com.chiyuan.va.fake.hook.MethodHook;
import com.chiyuan.va.fake.hook.ProxyMethod;
import com.chiyuan.va.utils.Slog;

/**
 * WebViewProxy — 负责在 VA 分身进程中修复 WebView 的两个核心问题：
 *
 *  1. ERR_CACHE_MISS：WebView 数据/缓存目录未正确隔离，导致 Chromium
 *     网络栈初始化失败，所有资源均报 net::ERR_CACHE_MISS。
 *
 *  2. setDataDirectorySuffix 含非法字符（冒号），使 Chromium 内部路径
 *     解析出错。
 *
 * 注意：ClassInvocationStub 仅适用于 Binder 服务代理（getWho() 须返回
 * 非 null）。WebView 不是 Binder 服务，此处改为仅提供静态初始化方法，
 * 由 BActivityThread.handleBindApplication 直接调用。
 */
public class WebViewProxy extends ClassInvocationStub {

    public static final String TAG = "WebViewProxy";

    // ---------------------------------------------------------------
    // ClassInvocationStub 接口（WebView 不走 Binder 代理，保持空实现）
    // ---------------------------------------------------------------

    @Override
    protected Object getWho() {
        return null;  // WebView 不是 Binder 服务，无需代理
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) { }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // ---------------------------------------------------------------
    // 静态初始化入口 — 由 BActivityThread.handleBindApplication 调用
    // ---------------------------------------------------------------

    /**
     * 在 handleBindApplication 中、IOCore.enableRedirect 之前调用。
     *
     * 作用：
     *  - 用无冒号的安全后缀隔离 WebView 数据目录（Android P+）
     *  - 预建缓存目录，使 Chromium 首次访问时不报 ERR_CACHE_MISS
     *  - 对已存在的 WebView 实例补充网络/缓存设置
     */
    public static void initForProcess(String packageName, String processName, int userId) {
        // 1. 安全的数据目录后缀（只含字母数字下划线）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String safeSuffix = userId
                    + "_" + packageName.replace('.', '_')
                    + "_" + processName.replace('.', '_').replace(':', '_');
            try {
                WebView.setDataDirectorySuffix(safeSuffix);
                Slog.d(TAG, "setDataDirectorySuffix -> " + safeSuffix);
            } catch (Throwable t) {
                // WebView 已初始化时调用会抛 IllegalStateException，忽略
                Slog.w(TAG, "setDataDirectorySuffix ignored: " + t.getMessage());
            }
        }

        // 2. 预建 WebView 缓存目录，避免 Chromium 因目录缺失回退到
        //    仅缓存模式（进而触发 ERR_CACHE_MISS）
        try {
            Context ctx = ChiyuanVACore.getContext();
            if (ctx != null) {
                File webViewCache = new File(ctx.getCacheDir(), "WebView/Default/HTTP Cache");
                if (!webViewCache.exists()) {
                    webViewCache.mkdirs();
                    Slog.d(TAG, "Pre-created WebView cache dir: " + webViewCache);
                }
            }
        } catch (Throwable t) {
            Slog.w(TAG, "Pre-create cache dir failed: " + t.getMessage());
        }
    }

    /**
     * 对一个已创建的 WebView 实例应用分身环境所需的设置。
     * 可在宿主 App 的 WebView 创建时通过反射注入调用，也可在
     * AppInstrumentation.callActivityOnCreate 中扫描 View 树后调用。
     */
    public static void configureWebViewInstance(WebView webView) {
        if (webView == null) return;
        try {
            WebSettings s = webView.getSettings();
            if (s == null) return;

            // 允许 JS（GeeTest 等验证码必需）
            s.setJavaScriptEnabled(true);
            // 关闭缓存优先，直接走网络，规避 ERR_CACHE_MISS
            s.setCacheMode(WebSettings.LOAD_NO_CACHE);
            // DOM Storage / Database（部分 H5 功能依赖）
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            // 允许混合内容（file:// 加载 https 资源）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            // 允许 file:// URL 跨域（asset 内联页需要）
            s.setAllowFileAccess(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                s.setAllowFileAccessFromFileURLs(true);
                s.setAllowUniversalAccessFromFileURLs(true);
            }
            // 强制声明网络可用（部分 Chromium 版本会在 VA 环境中误判为离线）
            try {
                webView.setNetworkAvailable(true);
            } catch (Throwable ignored) { }
            // 关闭 Safe Browsing（VA 环境下 Safe Browsing 服务不可达）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                s.setSafeBrowsingEnabled(false);
            }
            Slog.d(TAG, "configureWebViewInstance OK");
        } catch (Throwable t) {
            Slog.w(TAG, "configureWebViewInstance failed: " + t.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // 保留 @ProxyMethod 外壳（HookManager 扫描时不报错），但实际逻辑
    // 已移入上方静态方法
    // ---------------------------------------------------------------

    @ProxyMethod("setDataDirectorySuffix")
    public static class SetDataDirectorySuffix extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // getWho()==null → 此 hook 永远不会被 ClassInvocationStub 调用
            // 真正的修复在 initForProcess() 中完成
            return method.invoke(who, args);
        }
    }
}