package com.vogella.android.navigationwidgetattempt;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.Wisam.POJO.StatusResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class LocationService extends IntentService {
    private static final String TAG = "Location Service";
    //    // TODO: Rename actions, choose action names that describe tasks that this
//    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
//    public static final String ACTION_FOO = "com.vogella.android.navigationwidgetattempt.action.FOO";
//    public static final String ACTION_BAZ = "com.vogella.android.navigationwidgetattempt.action.BAZ";
//
//    // TODO: Rename parameters
//    public static final String EXTRA_PARAM1 = "com.vogella.android.navigationwidgetattempt.extra.PARAM1";
//    public static final String EXTRA_PARAM2 = "com.vogella.android.navigationwidgetattempt.extra.PARAM2";
//
    private PrefManager prefManager;

    public LocationService() {
        super("LocationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        prefManager = new PrefManager(this);
        Log.d(TAG,"This has been called!");
        if(intent.hasExtra("alarmType")){
            Log.d(TAG,"intent has alarmType");
//            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//            PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            PendingIntent pi2 = PendingIntent.getService(getApplicationContext(), 67769, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            alarmManager.cancel(pi);
//            alarmManager.cancel(pi2);

            if(isLocationEnabled(this)) {
                Intent locationIntent = new Intent(getApplicationContext(), UpdateLocation_Active.class);
                locationIntent.putExtra("alarmType", "location");
                PendingIntent locationPI = PendingIntent.getBroadcast(getApplicationContext(), 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager m = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                ;
                int intervalTimeMillis;
                if (prefManager.isDoingRequest())
                    intervalTimeMillis = 10 * 1000;  // 10 seconds
                else
                    intervalTimeMillis = 30 * 60 * 1000;//5 * 60 * 1000; // 5 minutes
                m.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalTimeMillis, locationPI);
                Intent activeIntent = new Intent(getApplicationContext(), UpdateLocation_Active.class);
                activeIntent.putExtra("alarmType", "active");

                PendingIntent activePI = PendingIntent.getBroadcast(getApplicationContext(), 67769, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                intervalTimeMillis = 30 * 1000;
                m.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + intervalTimeMillis, activePI);
            }

            if(intent.getStringExtra("alarmType").equals("location")){
                Log.d(TAG,"alarmType is location");
                String location = prefManager.getCurrentLocation();
                String request_id;
                if(prefManager.isDoingRequest())
                    request_id = prefManager.getRequestId();
                else
                    request_id = "-1";
                sendLocation(request_id, location);
            }
            else if(intent.getStringExtra("alarmType").equals("active")){
                Log.d(TAG,"alarmType is active");
                String location = prefManager.getCurrentLocation();
                int active;
                if(prefManager.isActive())
                    active = 1;
                else
                    active = 0;
                sendActive(active, location);
            }
            else {
                Log.d(TAG,"alarmType is : " + intent.getStringExtra("alarmType"));
            }
        }
        else {
            Log.d(TAG,"The intent doesn't have alarmType extra");
        }

    }
    private void sendLocation(String request_id, final String location ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
              String email = prefManager.pref.getString("UserEmail","");
        String password = prefManager.pref.getString("UserPassword","");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.location("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                request_id,location);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    Log.d(TAG, "The location has been sent successfully");
                } else if (response.code() == 401){
//                    Toast.makeText(MainActivity.this, "Please login to continue", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
//                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//                    MainActivity.this.startActivity(intent);
//                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
//                    Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                Log.i(TAG, "Failed to get the server");

            }
        });
    }

//    /**
//     * Handle action Foo in the provided background thread with the provided
//     * parameters.
//     */
//    private void handleActionFoo(String param1, String param2) {
//        // TODO: Handle action Foo
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//
//    /**
//     * Handle action Baz in the provided background thread with the provided
//     * parameters.
//     */
//    private void handleActionBaz(String param1, String param2) {
//        // TODO: Handle action Baz
//        throw new UnsupportedOperationException("Not yet implemented");
//    }

    private void sendActive(int active, final String location) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.active("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                active, location);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
                    Log.d(TAG, "The driver status has been changed successfully");
                } else if (response.code() == 401) {
//                    Toast.makeText(MainActivity.this, "Please login to continue", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "sendActive User not logged in");
                    prefManager.setIsLoggedIn(false);
//                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//                    MainActivity.this.startActivity(intent);
//                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
//                    Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "sendActive: Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {

            }
        });
    }
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

}
