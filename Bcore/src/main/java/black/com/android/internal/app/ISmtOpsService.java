package black.com.android.internal.app;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("com.android.internal.app.ISmtOpsService")
public interface ISmtOpsService {
    @BClassName("com.android.internal.app.ISmtOpsService$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
