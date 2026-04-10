package black.android.app;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("android.app.usage.IUsageStatsManager")
public interface IUsageStatsManager {
    @BClassName("android.app.usage.IUsageStatsManager$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
