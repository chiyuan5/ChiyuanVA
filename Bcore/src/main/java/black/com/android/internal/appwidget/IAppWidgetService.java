package black.com.android.internal.appwidget;

import android.os.IBinder;
import android.os.IInterface;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BStaticMethod;

@BClassName("com.android.internal.appwidget.IAppWidgetService")
public interface IAppWidgetService {
    @BClassName("com.android.internal.appwidget.IAppWidgetService$Stub")
    interface Stub {
        @BStaticMethod
        IInterface asInterface(IBinder IBinder0);
    }
}
