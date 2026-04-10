package com.chiyuan.va.core.system.pm.installer;

import com.chiyuan.va.core.env.BEnvironment;
import com.chiyuan.va.core.system.pm.BPackageSettings;
import com.chiyuan.va.entity.pm.InstallOption;
import com.chiyuan.va.utils.FileUtils;


public class RemoveAppExecutor implements Executor {
    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        FileUtils.deleteDir(BEnvironment.getAppDir(ps.pkg.packageName));
        return 0;
    }
}
