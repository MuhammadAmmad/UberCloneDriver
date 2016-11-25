package com.vogella.android.navigationwidgetattempt;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by nezuma on 11/25/16.
 */

public class UpdateLocation extends WakefulBroadcastReceiver{
    private static final String TAG = "Update Location";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"This has been called!");
        Intent service = new Intent(context, LocationService.class);
        startWakefulService(context, service);
    }
}
