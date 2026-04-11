package com.chiyuan.va.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.File;

import com.chiyuan.va.ChiyuanVACore;
import com.chiyuan.va.core.env.BEnvironment;

public class GuestAppContext extends ContextWrapper {
    private final Context fallbackContext;
    private final ApplicationInfo appInfo;
    private final int userId;

    public GuestAppContext(Context base, ApplicationInfo appInfo, int userId) {
        super(base != null ? base : ChiyuanVACore.getContext());
        this.fallbackContext = ChiyuanVACore.getContext();
        this.appInfo = appInfo;
        this.userId = userId;
        ensureGuestDirs();
    }

    private void ensureGuestDirs() {
        FileUtils.mkdirs(getDataDirCompat());
        FileUtils.mkdirs(getFilesDir());
        FileUtils.mkdirs(getCacheDir());
        FileUtils.mkdirs(getCodeCacheDir());
        FileUtils.mkdirs(getNoBackupFilesDir());
        FileUtils.mkdirs(new File(getDataDirCompat(), "shared_prefs"));
        FileUtils.mkdirs(getDatabaseBaseDir());
    }

    @Override
    public Context getBaseContext() {
        Context base = super.getBaseContext();
        return base != null ? base : fallbackContext;
    }

    @Override
    public String getPackageName() {
        return appInfo != null && appInfo.packageName != null
                ? appInfo.packageName
                : (getBaseContext() != null ? getBaseContext().getPackageName() : "unknown");
    }

    public String getOpPackageName() {
        return getPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        if (appInfo != null) {
            return appInfo;
        }
        return getBaseContext().getApplicationInfo();
    }

    @Override
    public PackageManager getPackageManager() {
        return getBaseContext().getPackageManager();
    }

    @Override
    public Resources getResources() {
        return getBaseContext().getResources();
    }

    @Override
    public AssetManager getAssets() {
        return getBaseContext().getAssets();
    }

    @Override
    public ClassLoader getClassLoader() {
        return getBaseContext().getClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public File getDataDir() {
        return getDataDirCompat();
    }

    private File getDataDirCompat() {
        if (appInfo != null && appInfo.dataDir != null) {
            return new File(appInfo.dataDir);
        }
        return BEnvironment.getDataDir(getPackageName(), userId);
    }

    private File getDatabaseBaseDir() {
        return new File(getDataDirCompat(), "databases");
    }

    @Override
    public File getFilesDir() {
        File dir = new File(getDataDirCompat(), "files");
        FileUtils.mkdirs(dir);
        return dir;
    }

    @Override
    public File getCacheDir() {
        File dir = new File(getDataDirCompat(), "cache");
        FileUtils.mkdirs(dir);
        return dir;
    }

    @Override
    public File getCodeCacheDir() {
        File dir = new File(getDataDirCompat(), "code_cache");
        FileUtils.mkdirs(dir);
        return dir;
    }

    @Override
    public File getNoBackupFilesDir() {
        File dir = new File(getDataDirCompat(), "no_backup");
        FileUtils.mkdirs(dir);
        return dir;
    }

    @Override
    public File getDir(String name, int mode) {
        File dir = new File(getDataDirCompat(), "app_" + name);
        FileUtils.mkdirs(dir);
        return dir;
    }

    @Override
    public File getDatabasePath(String name) {
        File dbDir = getDatabaseBaseDir();
        FileUtils.mkdirs(dbDir);
        return new File(dbDir, name);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return super.getSharedPreferences(name, mode);
    }

    @Override
    public String getPackageCodePath() {
        if (appInfo != null && appInfo.sourceDir != null) {
            return appInfo.sourceDir;
        }
        return getBaseContext().getPackageCodePath();
    }

    @Override
    public String getPackageResourcePath() {
        if (appInfo != null && appInfo.publicSourceDir != null) {
            return appInfo.publicSourceDir;
        }
        return getBaseContext().getPackageResourcePath();
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        return this;
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        return false;
    }

}
