package black.android.telephony;


import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BConstructor;
import com.chiyuan.va.reflection.annotation.BField;

@BClassName("android.telephony.CellSignalStrengthCdma")
public interface CellSignalStrengthCdma {
    @BConstructor
    CellSignalStrengthCdma _new();

    @BField
    int mCdmaDbm();

    @BField
    int mCdmaEcio();

    @BField
    int mEvdoDbm();

    @BField
    int mEvdoEcio();

    @BField
    int mEvdoSnr();
}
