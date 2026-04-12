package com.chiyuan.va.fake.service;

import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.chiyuan.va.fake.hook.BinderInvocationStub;
import com.chiyuan.va.fake.hook.MethodHook;
import com.chiyuan.va.fake.hook.ProxyMethod;
import com.chiyuan.va.utils.Slog;

public class IWebViewUpdateServiceProxy extends BinderInvocationStub {
    public static final String TAG = "IWebViewUpdateServiceProxy";
    private static final String WEBVIEW_UPDATE_SERVICE = "webviewupdate";

    public IWebViewUpdateServiceProxy() {
        super(BRServiceManager.get().getService(WEBVIEW_UPDATE_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService(WEBVIEW_UPDATE_SERVICE);
        if (binder == null) {
            Slog.w(TAG, "Failed to get webviewupdate binder");
            return null;
        }
        try {
            Class<?> stubClass = Class.forName("android.webkit.IWebViewUpdateService$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object iface = asInterfaceMethod.invoke(null, binder);
            if (iface instanceof IInterface) {
                Slog.d(TAG, "Successfully obtained IWebViewUpdateService interface");
                return iface;
            }
            Slog.w(TAG, "IWebViewUpdateService interface is null or invalid");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IWebViewUpdateService interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(WEBVIEW_UPDATE_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("isMultiProcessEnabled")
    public static class IsMultiProcessEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            Slog.d(TAG, "WebViewUpdateService: forcing single-process mode");
            return false;
        }
    }
}
