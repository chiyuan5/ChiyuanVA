package com.chiyuan.va.core.system;

import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.core.system.accounts.BAccountManagerService;
import com.chiyuan.va.core.system.am.BActivityManagerService;
import com.chiyuan.va.core.system.am.BJobManagerService;
import com.chiyuan.va.core.system.location.BLocationManagerService;
import com.chiyuan.va.core.system.notification.BNotificationManagerService;
import com.chiyuan.va.core.system.os.BStorageManagerService;
import com.chiyuan.va.core.system.pm.BPackageManagerService;

import com.chiyuan.va.core.system.user.BUserManagerService;


public class ServiceManager {
    private static ServiceManager sServiceManager = null;
    public static final String ACTIVITY_MANAGER = "activity_manager";
    public static final String JOB_MANAGER = "job_manager";
    public static final String PACKAGE_MANAGER = "package_manager";
    public static final String STORAGE_MANAGER = "storage_manager";
    public static final String USER_MANAGER = "user_manager";

    public static final String ACCOUNT_MANAGER = "account_manager";
    public static final String LOCATION_MANAGER = "location_manager";
    public static final String NOTIFICATION_MANAGER = "notification_manager";

    private final Map<String, IBinder> mCaches = new HashMap<>();

    public static ServiceManager get() {
        if (sServiceManager == null) {
            synchronized (ServiceManager.class) {
                if (sServiceManager == null) {
                    sServiceManager = new ServiceManager();
                }
            }
        }
        return sServiceManager;
    }

    public static IBinder getService(String name) {
        return get().getServiceInternal(name);
    }

    private ServiceManager() {
        mCaches.put(ACTIVITY_MANAGER, BActivityManagerService.get());
        mCaches.put(JOB_MANAGER, BJobManagerService.get());
        mCaches.put(PACKAGE_MANAGER, BPackageManagerService.get());
        mCaches.put(STORAGE_MANAGER, BStorageManagerService.get());
        mCaches.put(USER_MANAGER, BUserManagerService.get());

        mCaches.put(ACCOUNT_MANAGER, BAccountManagerService.get());
        mCaches.put(LOCATION_MANAGER, BLocationManagerService.get());
        mCaches.put(NOTIFICATION_MANAGER, BNotificationManagerService.get());
    }

    public IBinder getServiceInternal(String name) {
        return mCaches.get(name);
    }

    public static void initBlackManager() {
        ChiyuanVACore.get().getService(ACTIVITY_MANAGER);
        ChiyuanVACore.get().getService(JOB_MANAGER);
        ChiyuanVACore.get().getService(PACKAGE_MANAGER);
        ChiyuanVACore.get().getService(STORAGE_MANAGER);
        ChiyuanVACore.get().getService(USER_MANAGER);

        ChiyuanVACore.get().getService(ACCOUNT_MANAGER);
        ChiyuanVACore.get().getService(LOCATION_MANAGER);
        ChiyuanVACore.get().getService(NOTIFICATION_MANAGER);
    }
}
