package com.chiyuan.va.core.system;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.core.env.AppSystemEnv;
import com.chiyuan.va.core.env.BEnvironment;
import com.chiyuan.va.core.system.accounts.BAccountManagerService;
import com.chiyuan.va.core.system.am.BActivityManagerService;
import com.chiyuan.va.core.system.am.BJobManagerService;
import com.chiyuan.va.core.system.location.BLocationManagerService;
import com.chiyuan.va.core.system.notification.BNotificationManagerService;
import com.chiyuan.va.core.system.os.BStorageManagerService;
import com.chiyuan.va.core.system.pm.BPackageInstallerService;
import com.chiyuan.va.core.system.pm.BPackageManagerService;

import com.chiyuan.va.core.system.user.BUserHandle;
import com.chiyuan.va.core.system.user.BUserManagerService;
import com.chiyuan.va.entity.pm.InstallOption;
import com.chiyuan.va.utils.FileUtils;

import com.chiyuan.va.core.system.JarManager;


public class ChiyuanVASystem {
    private static ChiyuanVASystem sChiyuanVASystem;
    private final List<ISystemService> mServices = new ArrayList<>();
    private final static AtomicBoolean isStartup = new AtomicBoolean(false);

    public static ChiyuanVASystem getSystem() {
        if (sChiyuanVASystem == null) {
            synchronized (ChiyuanVASystem.class) {
                if (sChiyuanVASystem == null) {
                    sChiyuanVASystem = new ChiyuanVASystem();
                }
            }
        }
        return sChiyuanVASystem;
    }

    public void startup() {
        if (isStartup.getAndSet(true))
            return;
        BEnvironment.load();

        mServices.add(BPackageManagerService.get());
        mServices.add(BUserManagerService.get());
        mServices.add(BActivityManagerService.get());
        mServices.add(BJobManagerService.get());
        mServices.add(BStorageManagerService.get());
        mServices.add(BPackageInstallerService.get());

        mServices.add(BProcessManagerService.get());
        mServices.add(BAccountManagerService.get());
        mServices.add(BLocationManagerService.get());
        mServices.add(BNotificationManagerService.get());

        for (ISystemService service : mServices) {
            service.systemReady();
        }

        List<String> preInstallPackages = AppSystemEnv.getPreInstallPackages();
        for (String preInstallPackage : preInstallPackages) {
            try {
                if (!BPackageManagerService.get().isInstalled(preInstallPackage, BUserHandle.USER_ALL)) {
                    PackageInfo packageInfo = ChiyuanVACore.getPackageManager().getPackageInfo(preInstallPackage, 0);
                    BPackageManagerService.get().installPackageAsUser(packageInfo.applicationInfo.sourceDir, InstallOption.installBySystem(), BUserHandle.USER_ALL);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        
        JarManager.getInstance().initializeAsync();
        
        
     
    }
}
