package com.chiyuan.va.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class KrWebViewCompatFix {
    private static final String TAG = "KrWebViewCompatFix";
    private static final String TARGET_ACTIVITY = "com.kr.android.core.webview.activity.KRWebViewActivity";
    private static final int MAX_RETRY = 30;
    private static final long RETRY_DELAY_MS = 120L;

    private KrWebViewCompatFix() {
    }

    public static void tryInstall(final Activity activity) {
        if (activity == null) return;
        if (!TARGET_ACTIVITY.equals(activity.getClass().getName())) return;

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            int retryCount = 0;

            @Override
            public void run() {
                try {
                    if (installInternal(activity)) {
                        Slog.d(TAG, "installed for " + activity.getClass().getName());
                        return;
                    }
                } catch (Throwable e) {
                    Slog.w(TAG, "install failed: " + e.getMessage(), e);
                    return;
                }

                retryCount++;
                if (retryCount < MAX_RETRY && !activity.isFinishing()) {
                    handler.postDelayed(this, RETRY_DELAY_MS);
                } else {
                    Slog.w(TAG, "install timeout for " + activity.getClass().getName());
                }
            }
        });
    }

    private static boolean installInternal(final Activity activity) throws Exception {
        final Object krWebView = readFieldValue(activity, "krWebView");
        if (krWebView == null) {
            return false;
        }

        final Field listenerField = findField(krWebView.getClass(), "mKrWebViewShowListener");
        if (listenerField == null) {
            return false;
        }
        listenerField.setAccessible(true);

        final Object originalListener = listenerField.get(krWebView);
        if (originalListener == null) {
            return false;
        }
        if (Proxy.isProxyClass(originalListener.getClass())) {
            return true;
        }

        final Class<?> listenerInterface = listenerField.getType();
        final Object proxy = Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class[]{listenerInterface},
                (obj, method, args) -> {
                    String methodName = method.getName();
                    if ("showFailed".equals(methodName) && args != null && args.length >= 2) {
                        int errorCode = safeInt(args[0]);
                        String message = String.valueOf(args[1]);
                        if (shouldIgnoreShowFailed(errorCode, message)) {
                            Slog.w(TAG, "ignore KR showFailed, code=" + errorCode + ", msg=" + message);
                            clearErrorState(krWebView);
                            return null;
                        }
                    }
                    try {
                        method.setAccessible(true);
                    } catch (Throwable ignored) {
                    }
                    return method.invoke(originalListener, args);
                }
        );

        listenerField.set(krWebView, proxy);
        return true;
    }

    private static boolean shouldIgnoreShowFailed(int errorCode, String message) {
        if (errorCode == -1 && message != null && message.contains("ERR_CACHE_MISS")) {
            return true;
        }
        return false;
    }

    private static void clearErrorState(Object krWebView) {
        trySetBooleanField(krWebView, "mWebIsError", false);

        Object errorTips = readFieldValue(krWebView, "tv_erro_tips");
        if (errorTips instanceof View) {
            ((View) errorTips).setVisibility(View.GONE);
        }

        Object rootView = readFieldValue(krWebView, "mRootView");
        if (rootView instanceof View) {
            ((View) rootView).invalidate();
        }

        Object webViewObj = readFieldValue(krWebView, "wb_agreement");
        if (webViewObj instanceof WebView) {
            WebView webView = (WebView) webViewObj;
            try {
                if (webView.getSettings() != null) {
                    webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void trySetBooleanField(Object target, String fieldName, boolean value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.setBoolean(target, value);
            }
        } catch (Throwable e) {
            Slog.w(TAG, "set boolean field failed: " + fieldName + ", " + e.getMessage());
        }
    }

    private static Object readFieldValue(Object target, String fieldName) {
        if (target == null) return null;
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) return null;
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable e) {
            Slog.w(TAG, "read field failed: " + fieldName + ", " + e.getMessage());
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static int safeInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Throwable e) {
            return Integer.MIN_VALUE;
        }
    }
}
