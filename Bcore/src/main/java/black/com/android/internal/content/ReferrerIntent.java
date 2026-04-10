package black.com.android.internal.content;

import android.content.Intent;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BConstructor;

@BClassName("com.android.internal.content.ReferrerIntent")
public interface ReferrerIntent {
    @BConstructor
    Intent _new(Intent Intent0, String String1);
}
