package black.android.telephony;


import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BMethod;

@BClassName("android.telephony.SmsManager")
public interface SmsManager {
    @BMethod
    Boolean getAutoPersisting();

    @BMethod
    void setAutoPersisting(boolean boolean0);
}
