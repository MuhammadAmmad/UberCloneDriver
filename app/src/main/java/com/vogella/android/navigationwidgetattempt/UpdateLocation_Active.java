package com.vogella.android.navigationwidgetattempt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by nezuma on 11/25/16.
 */

public class UpdateLocation_Active extends WakefulBroadcastReceiver{
    private static final String TAG = "Update Location";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"This has been called!");
        if(intent.hasExtra("alarmType")){
            Log.d(TAG,"intent has alarmType");
            if(intent.getStringExtra("alarmType").equals("location")){
                Log.d(TAG,"alarmType is location");
                Intent service = new Intent(context, LocationService.class);
                service.putExtra("alarmType","location");
                startWakefulService(context, service);
            }
            else if(intent.getStringExtra("alarmType").equals("active")){
                Log.d(TAG,"alarmType is active");
                Intent service = new Intent(context, LocationService.class);
                service.putExtra("alarmType","active");
                startWakefulService(context, service);
            }
            else {
                Log.d(TAG,"alarmType is : " + intent.getStringExtra("alarmType"));
            }
        }
        else {
            Log.d(TAG,"The intent doesn't have alarmType extra");
        }

    }
}
