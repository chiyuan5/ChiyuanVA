package black.android.content;

import android.os.Bundle;
import android.os.IBinder;

import com.chiyuan.va.reflection.annotation.BClassName;
import com.chiyuan.va.reflection.annotation.BConstructor;
import com.chiyuan.va.reflection.annotation.BField;
import com.chiyuan.va.reflection.annotation.BMethod;
import com.chiyuan.va.reflection.annotation.BParamClassName;

@BClassName("android.content.BroadcastReceiver")
public interface BroadcastReceiver {
    @BMethod
    android.content.BroadcastReceiver.PendingResult getPendingResult();

    @BMethod
    void setPendingResult(@BParamClassName("android.content.BroadcastReceiver$PendingResult") Object pendingResult);

    @BClassName("android.content.BroadcastReceiver$PendingResult")
    interface PendingResultM {
        @BConstructor
        android.content.BroadcastReceiver.PendingResult _new(int resultCode, String resultData, Bundle resultExtras, int type,
                              boolean ordered, boolean sticky, IBinder token, int userId, int flags);

        @BField
        boolean mAbortBroadcast();

        @BField
        boolean mFinished();

        @BField
        int mFlags();

        @BField
        boolean mInitialStickyHint();

        @BField
        boolean mOrderedHint();

        @BField
        int mResultCode();

        @BField
        String mResultData();

        @BField
        Bundle mResultExtras();

        @BField
        int mSendingUser();

        @BField
        IBinder mToken();

        @BField
        int mType();
    }

    @BClassName("android.content.BroadcastReceiver$PendingResult")
    interface PendingResult {
        @BConstructor
        android.content.BroadcastReceiver.PendingResult _new(int resultCode, String resultData, Bundle resultExtras, int type,
                                                             boolean ordered, boolean sticky, IBinder token, int userId);

        @BField
        boolean mAbortBroadcast();

        @BField
        boolean mFinished();

        @BField
        boolean mInitialStickyHint();

        @BField
        boolean mOrderedHint();

        @BField
        int mResultCode();

        @BField
        String mResultData();

        @BField
        Bundle mResultExtras();

        @BField
        int mSendingUser();

        @BField
        IBinder mToken();

        @BField
        int mType();
    }
}
