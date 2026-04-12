package com.chiyuan.va.fake.service;

import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.chiyuan.va.fake.hook.BinderInvocationStub;
import com.chiyuan.va.fake.hook.MethodHook;
import com.chiyuan.va.fake.hook.ProxyMethod;
import com.chiyuan.va.utils.Slog;


public class IDnsResolverProxy extends BinderInvocationStub {
    public static final String TAG = "IDnsResolverProxy";
    public static final String DNS_RESOLVER_SERVICE = "dnsresolver";

    public IDnsResolverProxy() {
        super(BRServiceManager.get().getService(DNS_RESOLVER_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService(DNS_RESOLVER_SERVICE);
        if (binder == null) {
            Slog.w(TAG, "Failed to get dnsresolver binder");
            return null;
        }
        try {
            Class<?> stubClass = Class.forName("android.net.IDnsResolver$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object iface = asInterfaceMethod.invoke(null, binder);
            if (iface instanceof IInterface) {
                Slog.d(TAG, "Successfully obtained IDnsResolver interface");
                return iface;
            }
            Slog.w(TAG, "IDnsResolver interface is null or invalid");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IDnsResolver interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(DNS_RESOLVER_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("isAlive")
    public static class IsAlive extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }
}
