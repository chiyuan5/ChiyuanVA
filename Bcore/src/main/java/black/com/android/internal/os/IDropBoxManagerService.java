package black.com.android.internal.os;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("com.android.internal.os.IDropBoxManagerService")
public interface IDropBoxManagerService {
    @BClassName("com.android.internal.os.IDropBoxManagerService$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
