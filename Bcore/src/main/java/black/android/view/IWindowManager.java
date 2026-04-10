package black.android.view;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("android.view.IWindowManager")
public interface IWindowManager {
    @BClassName("android.view.IWindowManager$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
