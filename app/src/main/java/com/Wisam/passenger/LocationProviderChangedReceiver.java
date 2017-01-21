package com.Wisam.passenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.Wisam.Events.LocationDisabled;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by nezuma on 1/21/17.
 */

public class LocationProviderChangedReceiver  extends BroadcastReceiver {

    private static final String TAG = LocationProviderChangedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onRecieve called");
        EventBus.getDefault().post(new LocationDisabled());
    }

}


