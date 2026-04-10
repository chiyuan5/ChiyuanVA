package com.chiyuan.va.fake.service;

import android.content.pm.PackageInfo;
import android.os.IBinder;

import java.lang.reflect.Method;
import java.util.ArrayList;

import black.android.os.BRServiceManager;
import black.android.webkit.BRIWebViewUpdateServiceStub;
import com.chiyuan.va.fake.hook.BinderInvocationStub;
import com.chiyuan.va.fake.hook.MethodHook;
import com.chiyuan.va.fake.hook.ProxyMethod;
import com.chiyuan.va.utils.Slog;


public class IWebViewUpdateServiceProxy extends BinderInvocationStub {
    public static final String TAG = "IWebViewUpdateServiceProxy";
    public static final String SERVICE_NAME = "webviewupdate";

    public IWebViewUpdateServiceProxy() {
        super(getBinder());
    }

    private static IBinder getBinder() {
        try {
            return BRServiceManager.get().getService(SERVICE_NAME);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get webviewupdate binder", e);
            return null;
        }
    }

    @Override
    protected Object getWho() {
        try {
            IBinder binder = BRServiceManager.get().getService(SERVICE_NAME);
            if (binder == null) {
                Slog.e(TAG, "webviewupdate service binder is null");
                return null;
            }
            return BRIWebViewUpdateServiceStub.get().asInterface(binder);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IWebViewUpdateService", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(SERVICE_NAME);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getCurrentWebViewPackage")
    public static class GetCurrentWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "getCurrentWebViewPackage called");
            try {
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to get current WebView package", e);
            }
            return null;
        }
    }

    @ProxyMethod("getValidWebViewPackages")
    public static class GetValidWebViewPackages extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "getValidWebViewPackages called");
            try {
                Object result = method.invoke(who, args);
                if (result != null && result instanceof PackageInfo[]) {
                    PackageInfo[] packages = (PackageInfo[]) result;
                    if (packages.length > 0) {
                        return result;
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to get valid WebView packages", e);
            }
            return new PackageInfo[0];
        }
    }

    @ProxyMethod("isMultiProcessEnabled")
    public static class IsMultiProcessEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "isMultiProcessEnabled called, returning false");
            return false;
        }
    }

    @ProxyMethod("getWebViewPackages")
    public static class GetWebViewPackages extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "getWebViewPackages called");
            try {
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to get WebView packages", e);
            }
            return new ArrayList<>();
        }
    }

    @ProxyMethod("getWebViewProviderInfo")
    public static class GetWebViewProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "getWebViewProviderInfo called");
            try {
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to get WebView provider info", e);
            }
            return null;
        }
    }

    @ProxyMethod("isWebViewPackage")
    public static class IsWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "isWebViewPackage called for: " + packageName);
                if (isKnownWebViewPackage(packageName)) {
                    return true;
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Failed to check WebView package", e);
                return false;
            }
        }

        private boolean isKnownWebViewPackage(String packageName) {
            if (packageName == null) return false;
            return packageName.equals("com.google.android.webview") ||
                   packageName.equals("com.google.android.webview.dev") ||
                   packageName.equals("com.google.android.webview.beta") ||
                   packageName.equals("com.google.android.webview.canary") ||
                   packageName.equals("com.android.webview") ||
                   packageName.equals("com.huawei.webview") ||
                   packageName.equals("com.samsung.android.webview") ||
                   packageName.equals("com.oneplus.webview");
        }
    }

    @ProxyMethod("getWebViewProvider")
    public static class GetWebViewProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "getWebViewProvider called");
            try {
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to get WebView provider", e);
            }
            return null;
        }
    }

    @ProxyMethod("enableWebViewPackage")
    public static class EnableWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                Slog.d(TAG, "enableWebViewPackage called for: " + args[0]);
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Failed to enable WebView package", e);
                return true;
            }
        }
    }

    @ProxyMethod("disableWebViewPackage")
    public static class DisableWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                Slog.d(TAG, "disableWebViewPackage called for: " + args[0]);
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Failed to disable WebView package", e);
                return true;
            }
        }
    }
}
