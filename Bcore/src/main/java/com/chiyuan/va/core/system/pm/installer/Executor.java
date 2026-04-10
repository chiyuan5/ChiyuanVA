package com.chiyuan.va.core.system.pm.installer;

import com.chiyuan.va.core.system.pm.BPackageSettings;
import com.chiyuan.va.entity.pm.InstallOption;


public interface Executor {
    public static final String TAG = "InstallExecutor";

    int exec(BPackageSettings ps, InstallOption option, int userId);
}
