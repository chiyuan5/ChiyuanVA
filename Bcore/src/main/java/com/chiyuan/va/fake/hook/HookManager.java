package com.chiyuan.va.fake.hook;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.fake.delegate.AppInstrumentation;

import com.chiyuan.va.fake.service.HCallbackProxy;
import com.chiyuan.va.fake.service.IAccessibilityManagerProxy;
import com.chiyuan.va.fake.service.IAccountManagerProxy;
import com.chiyuan.va.fake.service.IActivityClientProxy;
import com.chiyuan.va.fake.service.IActivityManagerProxy;
import com.chiyuan.va.fake.service.IActivityTaskManagerProxy;
import com.chiyuan.va.fake.service.IAlarmManagerProxy;
import com.chiyuan.va.fake.service.IAppOpsManagerProxy;
import com.chiyuan.va.fake.service.IAppWidgetManagerProxy;
import com.chiyuan.va.fake.service.IAttributionSourceProxy;
import com.chiyuan.va.fake.service.IAutofillManagerProxy;
import com.chiyuan.va.fake.service.ISensitiveContentProtectionManagerProxy;
import com.chiyuan.va.fake.service.ISettingsSystemProxy;
import com.chiyuan.va.fake.service.IConnectivityManagerProxy;
import com.chiyuan.va.fake.service.ISystemSensorManagerProxy;
import com.chiyuan.va.fake.service.IContentProviderProxy;
import com.chiyuan.va.fake.service.IXiaomiAttributionSourceProxy;
import com.chiyuan.va.fake.service.IXiaomiSettingsProxy;
import com.chiyuan.va.fake.service.IXiaomiMiuiServicesProxy;
import com.chiyuan.va.fake.service.IDnsResolverProxy;
import com.chiyuan.va.fake.service.IContextHubServiceProxy;
import com.chiyuan.va.fake.service.IDeviceIdentifiersPolicyProxy;
import com.chiyuan.va.fake.service.IDevicePolicyManagerProxy;
import com.chiyuan.va.fake.service.IDisplayManagerProxy;
import com.chiyuan.va.fake.service.IFingerprintManagerProxy;
import com.chiyuan.va.fake.service.IGraphicsStatsProxy;
import com.chiyuan.va.fake.service.IJobServiceProxy;
import com.chiyuan.va.fake.service.ILauncherAppsProxy;
import com.chiyuan.va.fake.service.ILocationManagerProxy;
import com.chiyuan.va.fake.service.IMediaRouterServiceProxy;
import com.chiyuan.va.fake.service.IMediaSessionManagerProxy;
import com.chiyuan.va.fake.service.IAudioServiceProxy;
import com.chiyuan.va.fake.service.ISensorPrivacyManagerProxy;
import com.chiyuan.va.fake.service.ContentResolverProxy;
import com.chiyuan.va.fake.service.IWebViewUpdateServiceProxy;
import com.chiyuan.va.fake.service.IMiuiSecurityManagerProxy;
import com.chiyuan.va.fake.service.SystemLibraryProxy;
import com.chiyuan.va.fake.service.ReLinkerProxy;
import com.chiyuan.va.fake.service.WebViewProxy;
import com.chiyuan.va.fake.service.WebViewFactoryProxy;
import com.chiyuan.va.fake.service.MediaRecorderProxy;
import com.chiyuan.va.fake.service.AudioRecordProxy;
import com.chiyuan.va.fake.service.MediaRecorderClassProxy;
import com.chiyuan.va.fake.service.SQLiteDatabaseProxy;
import com.chiyuan.va.fake.service.ClassLoaderProxy;
import com.chiyuan.va.fake.service.FileSystemProxy;
import com.chiyuan.va.fake.service.GmsProxy;
import com.chiyuan.va.fake.service.LevelDbProxy;
import com.chiyuan.va.fake.service.DeviceIdProxy;
import com.chiyuan.va.fake.service.GoogleAccountManagerProxy;
import com.chiyuan.va.fake.service.AuthenticationProxy;
import com.chiyuan.va.fake.service.AndroidIdProxy;
import com.chiyuan.va.fake.service.AudioPermissionProxy;

import com.chiyuan.va.fake.service.INetworkManagementServiceProxy;
import com.chiyuan.va.fake.service.INotificationManagerProxy;
import com.chiyuan.va.fake.service.IPackageManagerProxy;
import com.chiyuan.va.fake.service.IPermissionManagerProxy;
import com.chiyuan.va.fake.service.IPersistentDataBlockServiceProxy;
import com.chiyuan.va.fake.service.IPhoneSubInfoProxy;
import com.chiyuan.va.fake.service.IPowerManagerProxy;
import com.chiyuan.va.fake.service.ApkAssetsProxy;
import com.chiyuan.va.fake.service.ResourcesManagerProxy;
import com.chiyuan.va.fake.service.IShortcutManagerProxy;
import com.chiyuan.va.fake.service.IStorageManagerProxy;
import com.chiyuan.va.fake.service.IStorageStatsManagerProxy;
import com.chiyuan.va.fake.service.ISystemUpdateProxy;
import com.chiyuan.va.fake.service.ITelephonyManagerProxy;
import com.chiyuan.va.fake.service.ITelephonyRegistryProxy;
import com.chiyuan.va.fake.service.IUserManagerProxy;
import com.chiyuan.va.fake.service.IVibratorServiceProxy;
import com.chiyuan.va.fake.service.IVpnManagerProxy;
import com.chiyuan.va.fake.service.IWifiManagerProxy;
import com.chiyuan.va.fake.service.IWifiScannerProxy;
import com.chiyuan.va.fake.service.IWindowManagerProxy;
import com.chiyuan.va.fake.service.context.ContentServiceStub;
import com.chiyuan.va.fake.service.context.RestrictionsManagerStub;
import com.chiyuan.va.fake.service.libcore.OsStub;
import com.chiyuan.va.utils.Slog;
import com.chiyuan.va.utils.compat.BuildCompat;
import com.chiyuan.va.fake.service.ISettingsProviderProxy;
import com.chiyuan.va.fake.service.FeatureFlagUtilsProxy;
import com.chiyuan.va.fake.service.WorkManagerProxy;



public class HookManager {
    public static final String TAG = "HookManager";

    private static final HookManager sHookManager = new HookManager();

    private final Map<Class<?>, IInjectHook> mInjectors = new HashMap<>();

    public static HookManager get() {
        return sHookManager;
    }

    public void init() {
        if (ChiyuanVACore.get().isBlackProcess() || ChiyuanVACore.get().isServerProcess()) {
            addInjector(new IDisplayManagerProxy());
            addInjector(new OsStub());
            addInjector(new IActivityManagerProxy());
            addInjector(new IPackageManagerProxy());
            addInjector(new ITelephonyManagerProxy());
            addInjector(new HCallbackProxy());
            addInjector(new IAppOpsManagerProxy());
            addInjector(new INotificationManagerProxy());
            addInjector(new IAlarmManagerProxy());
            addInjector(new IAppWidgetManagerProxy());
            addInjector(new ContentServiceStub());
            addInjector(new IWindowManagerProxy());
            addInjector(new IUserManagerProxy());
            addInjector(new RestrictionsManagerStub());
            addInjector(new IMediaSessionManagerProxy());
            addInjector(new IAudioServiceProxy());
            addInjector(new ISensorPrivacyManagerProxy());
            addInjector(new ContentResolverProxy());
            addInjector(new IWebViewUpdateServiceProxy());
            addInjector(new SystemLibraryProxy());
            addInjector(new ReLinkerProxy());
            addInjector(new WebViewProxy());
            addInjector(new WebViewFactoryProxy());
            addInjector(new WorkManagerProxy());
            addInjector(new MediaRecorderProxy());
            addInjector(new AudioRecordProxy());
            addInjector(new IMiuiSecurityManagerProxy());
            addInjector(new ISettingsProviderProxy());
            addInjector(new FeatureFlagUtilsProxy());
            addInjector(new MediaRecorderClassProxy());
            addInjector(new SQLiteDatabaseProxy());
            addInjector(new ClassLoaderProxy());
            addInjector(new FileSystemProxy());
            addInjector(new GmsProxy());
            addInjector(new LevelDbProxy());
            addInjector(new DeviceIdProxy());
            addInjector(new GoogleAccountManagerProxy());
            addInjector(new AuthenticationProxy());
            addInjector(new AndroidIdProxy());
            addInjector(new AudioPermissionProxy());
            addInjector(new ILocationManagerProxy());
            addInjector(new IStorageManagerProxy());
            addInjector(new ILauncherAppsProxy());
            addInjector(new IJobServiceProxy());
            addInjector(new IAccessibilityManagerProxy());
            addInjector(new ITelephonyRegistryProxy());
            addInjector(new IDevicePolicyManagerProxy());
            addInjector(new IAccountManagerProxy());
            addInjector(new IConnectivityManagerProxy());
            addInjector(new IDnsResolverProxy());
                    addInjector(new IAttributionSourceProxy());
        addInjector(new IContentProviderProxy());
        addInjector(new ISettingsSystemProxy());
        addInjector(new ISystemSensorManagerProxy());
        
        
        addInjector(new IXiaomiAttributionSourceProxy());
        addInjector(new IXiaomiSettingsProxy());
        addInjector(new IXiaomiMiuiServicesProxy());
            addInjector(new IPhoneSubInfoProxy());
            addInjector(new IMediaRouterServiceProxy());
            addInjector(new IPowerManagerProxy());
            addInjector(new IContextHubServiceProxy());
            
            addInjector(new IVibratorServiceProxy());
            addInjector(new IPersistentDataBlockServiceProxy());
            addInjector(AppInstrumentation.get());
            
            addInjector(new IWifiManagerProxy());
            addInjector(new IWifiScannerProxy());
            addInjector(new ApkAssetsProxy());
            addInjector(new ResourcesManagerProxy());
            
            if (BuildCompat.isS()) {
                addInjector(new IActivityClientProxy(null));
                addInjector(new IVpnManagerProxy());
            }
            
            if (BuildCompat.isS()) {
                addInjector(new ISensitiveContentProtectionManagerProxy());
            }
            
            if (BuildCompat.isR()) {
                addInjector(new IPermissionManagerProxy());
            }
            
            if (BuildCompat.isQ()) {
                addInjector(new IActivityTaskManagerProxy());
            }
            
            if (BuildCompat.isPie()) {
                addInjector(new ISystemUpdateProxy());
            }
            
            if (BuildCompat.isOreo()) {
                addInjector(new IAutofillManagerProxy());
                addInjector(new IDeviceIdentifiersPolicyProxy());
                addInjector(new IStorageStatsManagerProxy());
            }
            
            if (BuildCompat.isN_MR1()) {
                addInjector(new IShortcutManagerProxy());
            }
            
            if (BuildCompat.isN()) {
                addInjector(new INetworkManagementServiceProxy());
            }
            
            if (BuildCompat.isM()) {
                addInjector(new IFingerprintManagerProxy());
                addInjector(new IGraphicsStatsProxy());
            }
            
            if (BuildCompat.isL()) {
                addInjector(new IJobServiceProxy());
            }
        }
        injectAll();
    }

    public void checkEnv(Class<?> clazz) {
        IInjectHook iInjectHook = mInjectors.get(clazz);
        if (iInjectHook != null && iInjectHook.isBadEnv()) {
            Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env");
            iInjectHook.injectHook();
        }
    }

    public void checkAll() {
        for (Class<?> aClass : mInjectors.keySet()) {
            IInjectHook iInjectHook = mInjectors.get(aClass);
            if (iInjectHook != null && iInjectHook.isBadEnv()) {
                Log.d(TAG, "checkEnv: " + aClass.getSimpleName() + " is bad env");
                iInjectHook.injectHook();
            }
        }
    }

    void addInjector(IInjectHook injectHook) {
        mInjectors.put(injectHook.getClass(), injectHook);
    }

    void injectAll() {
        for (IInjectHook value : mInjectors.values()) {
            try {
                Slog.d(TAG, "hook: " + value);
                value.injectHook();
            } catch (Exception e) {
                Slog.d(TAG, "hook error: " + value);
                
                handleHookError(value, e);
            }
        }
    }

    
    private void handleHookError(IInjectHook hook, Exception e) {
        String hookName = hook.getClass().getSimpleName();
        
        
        Slog.e(TAG, "Hook failed: " + hookName + " - " + e.getMessage(), e);
        
        
        if (hookName.contains("ActivityManager") || 
            hookName.contains("PackageManager") ||
            hookName.contains("WebView") ||
            hookName.contains("ContentProvider")) {
            
            Slog.w(TAG, "Critical hook failed: " + hookName + ", attempting recovery");
            
            try {
                
                if (hook.isBadEnv()) {
                    Slog.d(TAG, "Attempting to recover hook: " + hookName);
                    hook.injectHook();
                }
            } catch (Exception recoveryException) {
                Slog.e(TAG, "Hook recovery failed: " + hookName, recoveryException);
            }
        }
    }

    
    public boolean areCriticalHooksInstalled() {
        String[] criticalHooks = {
            "IActivityManagerProxy",
            "IPackageManagerProxy", 
            "WebViewProxy",
            "IContentProviderProxy"
        };
        
        for (String hookName : criticalHooks) {
            boolean found = false;
            for (Class<?> hookClass : mInjectors.keySet()) {
                if (hookClass.getSimpleName().equals(hookName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Slog.w(TAG, "Critical hook missing: " + hookName);
                return false;
            }
        }
        
        Slog.d(TAG, "All critical hooks are installed");
        return true;
    }

    
    public void reinitializeHooks() {
        Slog.d(TAG, "Reinitializing all hooks");
        
        
        mInjectors.clear();
        
        
        init();
        
        Slog.d(TAG, "Hook reinitialization completed");
    }
}
