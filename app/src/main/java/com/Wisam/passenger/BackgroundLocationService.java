package com.Wisam.passenger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.Wisam.Events.ChangeActiveUpdateInterval;
import com.Wisam.Events.DriverActive;
import com.Wisam.Events.LocationUpdated;
import com.Wisam.Events.UnbindBackgroundLocationService;
import com.Wisam.POJO.StatusResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.Wisam.passenger.MainActivity.ACTIVE_NOTIFICATION_ID;
import static com.Wisam.passenger.MainActivity.REQUEST_CHECK_SETTINGS;

/**
 *
 * BackgroundLocationService used for tracking user location in the background.
 * @author cblack
 */
public class BackgroundLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult> {

    protected static final int ACCESS_FINE_LOCATION_CODE = 3124;
    private static final int RESENDING_ATTEMPTS = 20;
    IBinder mBinder = new LocalBinder();

    private GoogleApiClient mGoogleApiClient;
    private PowerManager.WakeLock mWakeLock;
    private LocationRequest mLocationRequest;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;

    private Boolean servicesAvailable = false;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    private static final String TAG = BackgroundLocationService.class.getSimpleName(); //"BackgroundLocationServ";
    private LocationSettingsRequest mLocationSettingsRequest;
    private com.Wisam.passenger.PrefManager prefManager;
    private int inactiveAttempts = 0;
    private boolean sentInactiveSuccessfully;

    protected static Boolean mRequestingLocationUpdates;

    protected static boolean checkedLocation = false;

    protected static final int UPDATE_DURING_REQUEST = 10 * 1000; //10 seconds
    protected static final int UPDATE_WHILE_IDLE = 2 * 60 * 1000;
    private Handler updateActiveHandler;
    private Runnable updateActiveRunnable;
    private Handler resendActivehandler;
    private Handler resendLocationHandler;
    private long resendFailedRequestDelay = 1000;
    protected static boolean permissionIsRequested = false;
    private Handler checkLocationHandler;
    private Runnable checkLocationRunnable;
    private int resendActiveAttempts = 0;
    private int resendLocationAttempts = 0;

    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate()");

        mRequestingLocationUpdates = false;

        mInProgress = false;
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        setUpLocationClientIfNeeded();

        buildLocationSettingsRequest();

        prefManager = new PrefManager(BackgroundLocationService.this);

        sentInactiveSuccessfully = false;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(prefManager.getDriverName());
        builder.setContentText("Active");
        builder.setSmallIcon(R.drawable.ic_notification_logo);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();


        startForeground(ACTIVE_NOTIFICATION_ID, notification);

        EventBus.getDefault().register(this);
    }

    /*
     * Create a new location client, using the enclosing class to
     * handle callbacks.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient");
        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void buildLocationSettingsRequest() {
        Log.d(TAG, "buildLocationSettingsRequest");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Check if the device's location settings are adequate for the app's needs using the
     * {@link com.google.android.gms.location.SettingsApi#checkLocationSettings(GoogleApiClient,
     * LocationSettingsRequest)} method, with the results provided through a {@code PendingResult}.
     */
    protected void checkLocationSettings() {
        Log.d(TAG, "checkLocationSettings");
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    /**
     * The callback invoked when
     * {@link com.google.android.gms.location.SettingsApi#checkLocationSettings(GoogleApiClient,
     * LocationSettingsRequest)} is called. Examines the
     * {@link com.google.android.gms.location.LocationSettingsResult} object and determines if
     * location settings are adequate. If they are not, begins the process of presenting a location
     * settings dialog to the user.
     */
    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {
        Log.d(TAG, "locationSettingsResult");
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                checkedLocation = false;
                Log.i(TAG, "All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings");
                Intent intent = new Intent(BackgroundLocationService.this, LocationSettingCallback.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(LocationSettingCallback.activity, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                    }
                }, 1000);
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                prefManager.setActive(false);
                EventBus.getDefault().post(new DriverActive(false));
                EventBus.getDefault().post(new UnbindBackgroundLocationService());
                stopSelf();
                break;
        }
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d(TAG, "onStartCommand");

        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);

        /*
        WakeLock is reference counted so we don't want to create multiple WakeLocks. So do a check before initializing and acquiring.
        This will fix the "java.lang.Exception: WakeLock finalized while still held: MyWakeLock" error that you may find.
        */
        if (this.mWakeLock == null) { //**Added this
            this.mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        }

        if (!this.mWakeLock.isHeld()) { //**Added this
            this.mWakeLock.acquire();
        }

        if (mGoogleApiClient.isConnected() || mInProgress)
            return START_STICKY;

        setUpLocationClientIfNeeded();
        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting() && !mInProgress) {
            mInProgress = true;
            mGoogleApiClient.connect();
        }

        return START_STICKY;
    }


    private void setUpLocationClientIfNeeded() {
        if (mGoogleApiClient == null)
            buildGoogleApiClient();
    }

    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: mLocation: " + location.toString());

        EventBus.getDefault().post(new LocationUpdated(location));
        prefManager.setCurrentLocation(String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude()));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(!permissionIsRequested){
                ActivityCompat.requestPermissions(MainActivity.context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
                permissionIsRequested = true;
            }
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {

                Log.d(TAG, "requestLocationUpdates result is" + status.toString());

                mRequestingLocationUpdates = true;

                prefManager.setActive(true);

                checkLocation();

                activeUpdate();

                EventBus.getDefault().post(new DriverActive(true));
            }
        });

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void ChangeActiveUpdateInterval(ChangeActiveUpdateInterval event) {
        Log.d(TAG,"ChangeActiveUpdateInterval event received");
        activeUpdate();
    }

    private void activeUpdate() {
        Log.d(TAG,"activeUpdate invoked");
        if(updateActiveHandler!= null)
            updateActiveHandler.removeCallbacksAndMessages(null);
        else
            updateActiveHandler = new Handler();
        updateActiveRunnable = new Runnable() {
            @Override
            public void run() {
                    Log.d(TAG,"updateActiveHandler running");
                    int intervalTimeMillis;
                    if (prefManager.isDoingRequest())
                        intervalTimeMillis = UPDATE_DURING_REQUEST;  // 10 seconds
                    else
                        intervalTimeMillis = UPDATE_WHILE_IDLE;//5 * 60 * 1000; // 5 minutes
                    String location = prefManager.getCurrentLocation();
                    String request_id;
                    if (prefManager.isDoingRequest())
                        request_id = prefManager.getRequestId();
                    else
                        request_id = "-1";
                    resendLocationAttempts = 0;
                    sendLocation(request_id, location);
                    int active;
                    if (prefManager.isActive())
                        active = 1;
                    else
                        active = 0;
                    resendActiveAttempts = 0;
                    sendActive(active, location);
                    updateActiveHandler.postDelayed(updateActiveRunnable, intervalTimeMillis);
                }
        };
        updateActiveRunnable.run();
    }

    protected void checkLocation() {
        Log.d(TAG,"checkLocation called");
        if(checkLocationHandler == null)
            checkLocationHandler = new Handler();
        checkLocationRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"checkLocationHandler running");
                if(!isLocationEnabled(BackgroundLocationService.this)) {
                    checkLocationHandler.removeCallbacksAndMessages(null);
                    Log.d(TAG,"checkLocationHandler removeCallbacksAndMessages");
                    Intent intent1 = new Intent(BackgroundLocationService.this, PopupActivity.class);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent1);
                }
                else{
                    Log.d(TAG,"checkLocationHandler scheduled again");
                    checkLocationHandler.postDelayed(checkLocationRunnable,10 * 1000);
                }
            }
        };
        checkLocationHandler.postDelayed(checkLocationRunnable,10 * 1000);
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


    private void sendLocation(final String request_id, final String location ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail","");
        String password = prefManager.pref.getString("UserPassword","");

        Log.d(TAG,"sendLocation called");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.location("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                request_id,location);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    Log.d(TAG, "The location has been sent successfully");
                    if(resendLocationHandler != null)
                        resendLocationHandler.removeCallbacksAndMessages(null);
                } else if (response.code() == 401){
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    stopSelf();
                } else {
                    Log.i(TAG, "sendLocation Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                Log.i(TAG, "sendLocation Failed to get the server", t);
                Log.i(TAG, "sendActive call data was: " + call.toString());
                if(resendLocationHandler == null)
                    resendLocationHandler = new Handler();
                if(resendLocationAttempts < RESENDING_ATTEMPTS) {
                    resendLocationAttempts++;
                    resendLocationHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendLocation(request_id, location);
                        }
                    }, resendFailedRequestDelay);
                }

            }
        });
    }


    private void sendActive(final int active, final String location) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        Log.d(TAG, "sendActive called");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.active("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                active, location);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
                    Log.d(TAG, "The driver status has been changed successfully");
                    if(resendActivehandler != null)
                        resendActivehandler.removeCallbacksAndMessages(null);
                } else if (response.code() == 401) {
                    Log.i(TAG, "sendActive User not logged in");
                    prefManager.setIsLoggedIn(false);
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    stopSelf();
                } else {
                    Log.i(TAG, "sendActive: Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                Log.i(TAG, "sendActive Failed to get the server", t);
                Log.i(TAG, "sendActive call data was: " + call.toString());
                if(resendActivehandler == null)
                    resendActivehandler = new Handler();
                if(resendActiveAttempts < RESENDING_ATTEMPTS) {
                    resendActiveAttempts++;
                    resendActivehandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendActive(active, location);

                        }
                    }, resendFailedRequestDelay);
                }
                else{
                    Log.d(TAG,String.format("Couldn't connect to the server after %d attempts... Stopping now.", RESENDING_ATTEMPTS));
                }

            }
        });
    }

    @Override
    public void onDestroy() {
        // Turn off the request flag
        Log.d(TAG, "onDestroy");

        //stop handlers' runnables
        if(checkLocationHandler != null)
            checkLocationHandler.removeCallbacksAndMessages(null);
        if(resendLocationHandler != null)
            resendLocationHandler.removeCallbacksAndMessages(null);
        if(resendActivehandler != null)
            resendActivehandler.removeCallbacksAndMessages(null);
        if(updateActiveHandler != null)
            updateActiveHandler.removeCallbacksAndMessages(null);
        Log.d(TAG,"Handlers runnables were removed");

        this.mInProgress = false;

        if (this.mGoogleApiClient != null) {
            this.mGoogleApiClient.unregisterConnectionCallbacks(this);
            this.mGoogleApiClient.unregisterConnectionFailedListener(this);
            this.mGoogleApiClient.disconnect();
            // Destroy the current location client
            this.mGoogleApiClient = null;
        }
        // Display the connection status
        // Toast.makeText(this, DateFormat.getDateTimeInstance().format(new Date()) + ":
        // Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();

        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }

        mRequestingLocationUpdates = false;

        Log.d(TAG,"Calling super.onDestroy");

        super.onDestroy();
    }
    private void sendInactiveToLogout(final String location) {
        final int active = 0;
        Log.d(TAG, "sendInactiveToLogout");
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
                    Log.d(TAG, "The driver status has been set successfully");
                    prefManager.setDriver(new driver());
                    inactiveAttempts = 0;
                    sentInactiveSuccessfully = true;
                } else if (response.code() == 401) {
                    Log.i(TAG, "onResponse: User not logged in");
                    prefManager.setIsLoggedIn(false);
                } else {
                    Log.i(TAG, "Unknown error occurred");
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                Log.i(TAG, getString(R.string.server_timeout));
            }
        });
    }


    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: Connected");

        // Request location updates using static settings
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(!checkedLocation) {
            checkLocationSettings();
            checkedLocation = true;
        }
    }

    /*
 * Called by Location Services if the connection to the
 * location client drops because of an error.
 */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
        mGoogleApiClient.connect();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());

        if (connectionResult.hasResolution()) {

            // If no resolution is available, display an error dialog
        } else {

        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
//        rootIntent.
    }



}
