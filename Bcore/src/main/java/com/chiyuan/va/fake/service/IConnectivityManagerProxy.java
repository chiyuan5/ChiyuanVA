package com.chiyuan.va.fake.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.fake.hook.BinderInvocationStub;
import com.chiyuan.va.fake.hook.MethodHook;
import com.chiyuan.va.fake.hook.ProxyMethod;
import com.chiyuan.va.fake.hook.ScanClass;
import com.chiyuan.va.utils.Slog;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import black.android.net.BRIConnectivityManagerStub;
import black.android.os.BRServiceManager;

@ScanClass(VpnCommonProxy.class)
public class IConnectivityManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IConnectivityManagerProxy";

    public IConnectivityManagerProxy() {
        super(BRServiceManager.get().getService(Context.CONNECTIVITY_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIConnectivityManagerStub.get().asInterface(
                BRServiceManager.get().getService(Context.CONNECTIVITY_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    private static ConnectivityManager getHostConnectivityManager() {
        try {
            Context context = ChiyuanVACore.getContext();
            if (context != null) {
                return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            }
        } catch (Throwable e) {
            Slog.w(TAG, "getHostConnectivityManager failed: " + e.getMessage());
        }
        return null;
    }

    private static Network getHostActiveNetwork() {
        if (Build.VERSION.SDK_INT < 21) return null;
        try {
            ConnectivityManager cm = getHostConnectivityManager();
            if (cm != null) {
                return Build.VERSION.SDK_INT >= 23 ? cm.getActiveNetwork() : null;
            }
        } catch (Throwable e) {
            Slog.w(TAG, "getHostActiveNetwork failed: " + e.getMessage());
        }
        return null;
    }

    private static NetworkInfo getHostActiveNetworkInfo() {
        try {
            ConnectivityManager cm = getHostConnectivityManager();
            if (cm != null) {
                return cm.getActiveNetworkInfo();
            }
        } catch (Throwable e) {
            Slog.w(TAG, "getHostActiveNetworkInfo failed: " + e.getMessage());
        }
        return null;
    }

    private static LinkProperties getHostLinkProperties(Network network) {
        if (Build.VERSION.SDK_INT < 21) return null;
        try {
            ConnectivityManager cm = getHostConnectivityManager();
            if (cm == null) return null;
            if (network == null && Build.VERSION.SDK_INT >= 23) {
                network = cm.getActiveNetwork();
            }
            if (network != null) {
                return cm.getLinkProperties(network);
            }
        } catch (Throwable e) {
            Slog.w(TAG, "getHostLinkProperties failed: " + e.getMessage());
        }
        return null;
    }

    private static NetworkCapabilities getHostNetworkCapabilities(Network network) {
        if (Build.VERSION.SDK_INT < 21) return null;
        try {
            ConnectivityManager cm = getHostConnectivityManager();
            if (cm == null) return null;
            if (network == null && Build.VERSION.SDK_INT >= 23) {
                network = cm.getActiveNetwork();
            }
            if (network != null) {
                return cm.getNetworkCapabilities(network);
            }
        } catch (Throwable e) {
            Slog.w(TAG, "getHostNetworkCapabilities failed: " + e.getMessage());
        }
        return null;
    }

    private static Object invokeOriginal(Object who, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(who, args);
        } catch (Throwable e) {
            Throwable cause = e.getCause();
            throw cause != null ? cause : e;
        }
    }

    private static NetworkInfo ensureConnectedNetworkInfo(Object value) {
        if (!(value instanceof NetworkInfo)) return null;
        NetworkInfo info = (NetworkInfo) value;
        try {
            info.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
        } catch (Throwable ignored) {
        }
        return info;
    }

    private static NetworkInfo createFallbackNetworkInfo(int type, String typeName) {
        try {
            NetworkInfo info = new NetworkInfo(type, 0, typeName, "");
            info.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
            return info;
        } catch (Throwable e) {
            Slog.w(TAG, "createFallbackNetworkInfo failed: " + e.getMessage());
            return null;
        }
    }

    private static NetworkInfo[] createFallbackNetworkInfoArray() {
        List<NetworkInfo> result = new ArrayList<>();
        NetworkInfo host = ensureConnectedNetworkInfo(getHostActiveNetworkInfo());
        if (host != null) {
            result.add(host);
        }
        if (result.isEmpty()) {
            NetworkInfo wifi = createFallbackNetworkInfo(ConnectivityManager.TYPE_WIFI, "WIFI");
            if (wifi != null) result.add(wifi);
        }
        return result.toArray(new NetworkInfo[0]);
    }

    private static Object createSingleNetworkArray(Network network) {
        if (network == null) return null;
        try {
            Object array = Array.newInstance(Network.class, 1);
            Array.set(array, 0, network);
            return array;
        } catch (Throwable e) {
            Slog.w(TAG, "createSingleNetworkArray failed: " + e.getMessage());
            return null;
        }
    }

    private static Network createFallbackNetwork() {
        Network host = getHostActiveNetwork();
        if (host != null) return host;
        try {
            Constructor<Network> constructor = Network.class.getConstructor(int.class);
            return constructor.newInstance(1);
        } catch (Throwable e) {
            Slog.w(TAG, "createFallbackNetwork failed: " + e.getMessage());
            return null;
        }
    }

    private static void invokeNetworkCapabilitiesIntMethod(NetworkCapabilities nc, String methodName, int value) {
        if (nc == null) return;
        try {
            Method method = findMethodRecursive(NetworkCapabilities.class, methodName, int.class);
            if (method != null) {
                method.invoke(nc, value);
            }
        } catch (Throwable e) {
            Slog.w(TAG, methodName + " failed: " + e.getMessage());
        }
    }

    private static NetworkCapabilities createFallbackNetworkCapabilities() {
        if (Build.VERSION.SDK_INT < 21) return null;
        try {
            NetworkCapabilities nc = new NetworkCapabilities();
            invokeNetworkCapabilitiesIntMethod(nc, "addTransportType", NetworkCapabilities.TRANSPORT_WIFI);
            invokeNetworkCapabilitiesIntMethod(nc, "addCapability", NetworkCapabilities.NET_CAPABILITY_INTERNET);
            invokeNetworkCapabilitiesIntMethod(nc, "addCapability", NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            invokeNetworkCapabilitiesIntMethod(nc, "addCapability", NetworkCapabilities.NET_CAPABILITY_TRUSTED);
            invokeNetworkCapabilitiesIntMethod(nc, "addCapability", NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
            invokeNetworkCapabilitiesIntMethod(nc, "addCapability", NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
            return nc;
        } catch (Throwable e) {
            Slog.w(TAG, "createFallbackNetworkCapabilities failed: " + e.getMessage());
            return null;
        }
    }

    private static LinkProperties createFallbackLinkProperties() {
        try {
            LinkProperties lp = new LinkProperties();
            List<InetAddress> dnsServers = new ArrayList<>();
            dnsServers.add(InetAddress.getByName("8.8.8.8"));
            dnsServers.add(InetAddress.getByName("8.8.4.4"));
            lp.setDnsServers(dnsServers);
            return lp;
        } catch (Throwable e) {
            Slog.w(TAG, "createFallbackLinkProperties failed: " + e.getMessage());
            return null;
        }
    }

    private static String getHostPrivateDnsServerName() {
        if (Build.VERSION.SDK_INT < 28) return null;
        try {
            ConnectivityManager cm = getHostConnectivityManager();
            if (cm == null) return null;
            Method m = ConnectivityManager.class.getMethod("getPrivateDnsServerName");
            Object value = m.invoke(cm);
            return value instanceof String ? (String) value : null;
        } catch (Throwable e) {
            Slog.w(TAG, "getHostPrivateDnsServerName failed: " + e.getMessage());
            return null;
        }
    }

    private static boolean isHostPrivateDnsActive() {
        if (Build.VERSION.SDK_INT < 28) return false;
        try {
            ConnectivityManager cm = getHostConnectivityManager();
            if (cm == null) return false;
            Method m = ConnectivityManager.class.getMethod("isPrivateDnsActive");
            Object value = m.invoke(cm);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable e) {
            Slog.w(TAG, "isHostPrivateDnsActive failed: " + e.getMessage());
            return false;
        }
    }

    private static List<InetAddress> getHostDnsServers() {
        try {
            LinkProperties lp = getHostLinkProperties(getHostActiveNetwork());
            if (lp != null && lp.getDnsServers() != null && !lp.getDnsServers().isEmpty()) {
                return lp.getDnsServers();
            }
        } catch (Throwable e) {
            Slog.w(TAG, "getHostDnsServers failed: " + e.getMessage());
        }

        List<InetAddress> fallback = new ArrayList<>();
        try {
            fallback.add(InetAddress.getByName("8.8.8.8"));
            fallback.add(InetAddress.getByName("8.8.4.4"));
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static Object findNetworkCallback(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg == null) continue;
            Class<?> clazz = arg.getClass();
            while (clazz != null) {
                String name = clazz.getName();
                if (name != null && name.contains("ConnectivityManager$NetworkCallback")) {
                    return arg;
                }
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethodRecursive(Class<?> clazz, String name, Class<?>... params) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
            }
            current = current.getSuperclass();
        }
        try {
            Method method = clazz.getMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void dispatchCurrentNetworkState(final Object callback) {
        if (callback == null || Build.VERSION.SDK_INT < 21) return;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Network network = getHostActiveNetwork();
                    if (network == null) {
                        network = createFallbackNetwork();
                    }
                    if (network == null) return;

                    Method onAvailable = findMethodRecursive(callback.getClass(), "onAvailable", Network.class);
                    if (onAvailable != null) {
                        onAvailable.invoke(callback, network);
                    }

                    NetworkCapabilities nc = getHostNetworkCapabilities(network);
                    if (nc == null) {
                        nc = createFallbackNetworkCapabilities();
                    }
                    if (nc != null) {
                        Method onCapabilitiesChanged = findMethodRecursive(
                                callback.getClass(),
                                "onCapabilitiesChanged",
                                Network.class,
                                NetworkCapabilities.class);
                        if (onCapabilitiesChanged != null) {
                            onCapabilitiesChanged.invoke(callback, network, nc);
                        }
                    }

                    LinkProperties lp = getHostLinkProperties(network);
                    if (lp == null) {
                        lp = createFallbackLinkProperties();
                    }
                    if (lp != null) {
                        Method onLinkPropertiesChanged = findMethodRecursive(
                                callback.getClass(),
                                "onLinkPropertiesChanged",
                                Network.class,
                                LinkProperties.class);
                        if (onLinkPropertiesChanged != null) {
                            onLinkPropertiesChanged.invoke(callback, network, lp);
                        }
                    }
                } catch (Throwable e) {
                    Slog.w(TAG, "dispatchCurrentNetworkState failed: " + e.getMessage());
                }
            }
        });
    }

    private static Object defaultValueFor(Method method) {
        Class<?> type = method.getReturnType();
        if (type == Void.TYPE) return null;
        if (type == Boolean.TYPE) return false;
        if (type == Integer.TYPE) return 0;
        if (type == Long.TYPE) return 0L;
        if (type == Float.TYPE) return 0f;
        if (type == Double.TYPE) return 0d;
        if (type == Short.TYPE) return (short) 0;
        if (type == Byte.TYPE) return (byte) 0;
        if (type == Character.TYPE) return (char) 0;
        return null;
    }

    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) {
                    return ensureConnectedNetworkInfo(result);
                }
            } catch (Throwable e) {
                Slog.w(TAG, "getNetworkInfo original failed: " + e.getMessage());
            }

            NetworkInfo host = ensureConnectedNetworkInfo(getHostActiveNetworkInfo());
            if (host != null) return host;
            return createFallbackNetworkInfo(ConnectivityManager.TYPE_WIFI, "WIFI");
        }
    }

    @ProxyMethod("getAllNetworkInfo")
    public static class GetAllNetworkInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null && Array.getLength(result) > 0) {
                    return result;
                }
            } catch (Throwable e) {
                Slog.w(TAG, "getAllNetworkInfo original failed: " + e.getMessage());
            }
            return createFallbackNetworkInfoArray();
        }
    }

    @ProxyMethod("getAllNetworks")
    public static class GetAllNetworks extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null && Array.getLength(result) > 0) {
                    return result;
                }
            } catch (Throwable e) {
                Slog.w(TAG, "getAllNetworks original failed: " + e.getMessage());
            }

            Object array = createSingleNetworkArray(getHostActiveNetwork());
            if (array != null) return array;
            return createSingleNetworkArray(createFallbackNetwork());
        }
    }

    @ProxyMethod("getNetworkCapabilities")
    public static class GetNetworkCapabilitiesHook extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "getNetworkCapabilities original failed: " + e.getMessage());
            }

            Network target = (args != null && args.length > 0 && args[0] instanceof Network)
                    ? (Network) args[0] : getHostActiveNetwork();
            NetworkCapabilities host = getHostNetworkCapabilities(target);
            return host != null ? host : createFallbackNetworkCapabilities();
        }
    }

    @ProxyMethod("getActiveNetwork")
    public static class GetActiveNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "getActiveNetwork original failed: " + e.getMessage());
            }

            Network host = getHostActiveNetwork();
            return host != null ? host : createFallbackNetwork();
        }
    }

    @ProxyMethod("getActiveNetworkInfo")
    public static class GetActiveNetworkInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return ensureConnectedNetworkInfo(result);
            } catch (Throwable e) {
                Slog.w(TAG, "getActiveNetworkInfo original failed: " + e.getMessage());
            }

            NetworkInfo host = ensureConnectedNetworkInfo(getHostActiveNetworkInfo());
            if (host != null) return host;
            return createFallbackNetworkInfo(ConnectivityManager.TYPE_WIFI, "WIFI");
        }
    }

    @ProxyMethod("getLinkProperties")
    public static class GetLinkProperties extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "getLinkProperties original failed: " + e.getMessage());
            }

            Network target = (args != null && args.length > 0 && args[0] instanceof Network)
                    ? (Network) args[0] : getHostActiveNetwork();
            LinkProperties host = getHostLinkProperties(target);
            return host != null ? host : createFallbackLinkProperties();
        }
    }

    @ProxyMethod("getPrivateDnsServerName")
    public static class GetPrivateDnsServerName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "getPrivateDnsServerName original failed: " + e.getMessage());
            }
            return getHostPrivateDnsServerName();
        }
    }

    @ProxyMethod("isPrivateDnsActive")
    public static class IsPrivateDnsActive extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return invokeOriginal(who, method, args);
            } catch (Throwable e) {
                Slog.w(TAG, "isPrivateDnsActive original failed: " + e.getMessage());
            }
            return isHostPrivateDnsActive();
        }
    }

    @ProxyMethod("getDnsServers")
    public static class GetDnsServers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "getDnsServers original failed: " + e.getMessage());
            }
            return getHostDnsServers();
        }
    }

    @ProxyMethod("isNetworkValidated")
    public static class IsNetworkValidated extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "isNetworkValidated original failed: " + e.getMessage());
            }
            return true;
        }
    }

    @ProxyMethod("requestNetwork")
    public static class RequestNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                Object callback = findNetworkCallback(args);
                dispatchCurrentNetworkState(callback);
                return result;
            } catch (Throwable e) {
                Slog.w(TAG, "requestNetwork original failed: " + e.getMessage());
                Object callback = findNetworkCallback(args);
                dispatchCurrentNetworkState(callback);
                return defaultValueFor(method);
            }
        }
    }

    @ProxyMethod("registerNetworkCallback")
    public static class RegisterNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object callback = findNetworkCallback(args);
            try {
                Object result = invokeOriginal(who, method, args);
                dispatchCurrentNetworkState(callback);
                return result;
            } catch (Throwable e) {
                Slog.w(TAG, "registerNetworkCallback original failed: " + e.getMessage());
                dispatchCurrentNetworkState(callback);
                return defaultValueFor(method);
            }
        }
    }

    @ProxyMethod("registerDefaultNetworkCallback")
    public static class RegisterDefaultNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object callback = findNetworkCallback(args);
            try {
                Object result = invokeOriginal(who, method, args);
                dispatchCurrentNetworkState(callback);
                return result;
            } catch (Throwable e) {
                Slog.w(TAG, "registerDefaultNetworkCallback original failed: " + e.getMessage());
                dispatchCurrentNetworkState(callback);
                return defaultValueFor(method);
            }
        }
    }

    @ProxyMethod("getActiveNetworkInfoForUid")
    public static class GetActiveNetworkInfoForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return ensureConnectedNetworkInfo(result);
            } catch (Throwable e) {
                Slog.w(TAG, "getActiveNetworkInfoForUid original failed: " + e.getMessage());
            }
            NetworkInfo host = ensureConnectedNetworkInfo(getHostActiveNetworkInfo());
            if (host != null) return host;
            return createFallbackNetworkInfo(ConnectivityManager.TYPE_WIFI, "WIFI");
        }
    }

    @ProxyMethod("addDefaultNetworkActiveListener")
    public static class AddDefaultNetworkActiveListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return invokeOriginal(who, method, args);
            } catch (Throwable e) {
                Slog.w(TAG, "addDefaultNetworkActiveListener original failed: " + e.getMessage());
                return defaultValueFor(method);
            }
        }
    }

    @ProxyMethod("removeDefaultNetworkActiveListener")
    public static class RemoveDefaultNetworkActiveListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return invokeOriginal(who, method, args);
            } catch (Throwable e) {
                Slog.w(TAG, "removeDefaultNetworkActiveListener original failed: " + e.getMessage());
                return defaultValueFor(method);
            }
        }
    }

    @ProxyMethod("isActiveNetworkMetered")
    public static class IsActiveNetworkMetered extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "isActiveNetworkMetered original failed: " + e.getMessage());
            }
            return false;
        }
    }

    @ProxyMethod("getNetworkForType")
    public static class GetNetworkForType extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = invokeOriginal(who, method, args);
                if (result != null) return result;
            } catch (Throwable e) {
                Slog.w(TAG, "getNetworkForType original failed: " + e.getMessage());
            }
            Network host = getHostActiveNetwork();
            return host != null ? host : createFallbackNetwork();
        }
    }

    @ProxyMethod("unregisterNetworkCallback")
    public static class UnregisterNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return invokeOriginal(who, method, args);
            } catch (Throwable e) {
                Slog.w(TAG, "unregisterNetworkCallback original failed: " + e.getMessage());
                return defaultValueFor(method);
            }
        }
    }
}
