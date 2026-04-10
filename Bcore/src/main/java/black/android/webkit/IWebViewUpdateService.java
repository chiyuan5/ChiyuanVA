package black.android.webkit;


import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BMethod;

@BClassName("android.webkit.IWebViewUpdateService")
public interface IWebViewUpdateService {
    @BMethod
    String getCurrentWebViewPackageName();

    @BMethod
    Object waitForAndGetProvider();
}
