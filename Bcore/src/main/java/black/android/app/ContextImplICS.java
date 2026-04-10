package black.android.app;

import java.io.File;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BField;

@BClassName("android.app.ContextImpl")
public interface ContextImplICS {
    @BField
    File mExternalCacheDir();

    @BField
    File mExternalFilesDir();
}
