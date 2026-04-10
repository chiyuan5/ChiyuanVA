package com.chiyuan.va.fake.service;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import black.android.view.accessibility.BRIAccessibilityManagerStub;
import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.core.system.user.BUserHandle;
import com.chiyuan.va.fake.hook.BinderInvocationStub;
import com.chiyuan.va.fake.hook.MethodHook;
import com.chiyuan.va.fake.hook.ProxyMethods;


public class IAccessibilityManagerProxy extends BinderInvocationStub {

    public IAccessibilityManagerProxy() {
        super(BRServiceManager.get().getService(Context.ACCESSIBILITY_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIAccessibilityManagerStub.get().asInterface(BRServiceManager.get().getService(Context.ACCESSIBILITY_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethods({"interrupt", "sendAccessibilityEvent", "addClient",
            "getInstalledAccessibilityServiceList", "getEnabledAccessibilityServiceList",
            "addAccessibilityInteractionConnection", "getWindowToken"})
    public static class ReplaceUserId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null) {
                int index = args.length - 1;
                Object arg = args[index];
                if (arg instanceof Integer) {
                    ApplicationInfo applicationInfo = ChiyuanVACore.getContext().getApplicationInfo();
                    args[index] = BUserHandle.getUserId(applicationInfo.uid);
                }
            }
            return method.invoke(who, args);
        }
    }
}
