package black.android.content.pm;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("mirror.android.content.pm.ILauncherApps")
public interface ILauncherApps {
    @BClassName("android.content.pm.ILauncherApps$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder binder);
    }
}
