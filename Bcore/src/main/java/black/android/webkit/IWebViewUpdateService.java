package black.android.webkit;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BMethod;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("android.webkit.IWebViewUpdateService")
public interface IWebViewUpdateService {
    @BMethod
    String getCurrentWebViewPackageName();

    @BMethod
    Object waitForAndGetProvider();

    @BClassName("android.webkit.IWebViewUpdateService$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
