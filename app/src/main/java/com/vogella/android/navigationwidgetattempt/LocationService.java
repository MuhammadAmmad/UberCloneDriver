package com.vogella.android.navigationwidgetattempt;

import android.app.IntentService;
import android.content.Intent;
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
        if (intent != null) {
            String location = prefManager.getCurrentLocation();
            String request_id;
            if(prefManager.isDoingRequest())
                request_id = prefManager.getRequestId();
            else
                request_id = "-1";
            sendLocation(request_id, location);
        }
    }
    private void sendLocation(String request_id, final String location ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("USER_EMAIL","");
        String password = prefManager.pref.getString("USER_PASSWORD","");

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
}
