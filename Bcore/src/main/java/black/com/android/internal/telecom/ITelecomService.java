package black.com.android.internal.telecom;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("com.android.internal.telecom.ITelecomService")
public interface ITelecomService {
    @BClassName("com.android.internal.telecom.ITelecomService$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
