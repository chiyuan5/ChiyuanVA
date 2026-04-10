package black.android.os.storage;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("android.os.storage.IStorageManager")
public interface IStorageManager {
    @BClassName("android.os.storage.IStorageManager$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
