package black.android.location.provider;


import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BField;

@BClassName("android.location.provider.ProviderProperties")
public interface ProviderProperties {
    @BField
    boolean mHasNetworkRequirement();

    @BField
    boolean mHasSatelliteRequirement();

    @BField
    boolean mHasCellRequirement();

    @BField
    boolean mHasMonetaryCost();

    @BField
    boolean mHasAltitudeSupport();

    @BField
    boolean mHasSpeedSupport();

    @BField
    boolean mHasBearingSupport();
}
