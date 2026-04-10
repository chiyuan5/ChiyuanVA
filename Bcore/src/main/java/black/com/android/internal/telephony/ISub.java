package black.com.android.internal.telephony;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("com.android.internal.telephony.ISub")
public interface ISub {
    @BClassName("com.android.internal.telephony.ISub$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
