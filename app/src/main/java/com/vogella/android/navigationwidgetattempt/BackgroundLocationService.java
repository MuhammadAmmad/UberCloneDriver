package com.vogella.android.navigationwidgetattempt;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.Wisam.Events.DriverActive;
import com.Wisam.Events.LocationUpdated;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.vogella.android.navigationwidgetattempt.MainActivity.ACTIVE_NOTIFICATION_ID;
import static com.vogella.android.navigationwidgetattempt.MainActivity.REQUEST_CHECK_SETTINGS;
import static com.vogella.android.navigationwidgetattempt.MainActivity.checkedLocation;
import static com.vogella.android.navigationwidgetattempt.MainActivity.mRequestingLocationUpdates;

/**
 *
 * BackgroundLocationService used for tracking user location in the background.
 * @author cblack
 */
public class BackgroundLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult> {

    private static final int ACCESS_FINE_LOCATION_CODE = 3124;
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
    private PrefManager prefManager;
    private int inactiveAttempts = 0;
    private boolean sentInactiveSuccessfully;


    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"onCreate()");

        mInProgress = false;
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

//        servicesAvailable = servicesConnected();

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        setUpLocationClientIfNeeded();

        buildLocationSettingsRequest();

        prefManager = new PrefManager(this);

        sentInactiveSuccessfully = false;

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

/*
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            return true;
        } else {

            return false;
        }
    }
*/

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
//        result.setResultCallback(MainActivity.context);
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
                Log.i(TAG, "All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings");
                checkedLocation = false;

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(MainActivity.context, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                checkedLocation = false;
                prefManager.setActive(false);
                EventBus.getDefault().post(new DriverActive(false));
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

//        if (!servicesAvailable || mGoogleApiClient.isConnected() || mInProgress)
        if (mGoogleApiClient.isConnected() || mInProgress)
            return START_STICKY;

        setUpLocationClientIfNeeded();
        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting() && !mInProgress) {
//            appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Started", Constants.LOG_FILE);
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
//        if(MainActivity.context.is)

//        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//        Log.d(TAG, "onLocationChanged: mLocation: " + location.toString());
//        if (firstMove && mLastLocation != null) {
//            Log.d(TAG, "onLocationChanged: Moving cam");
//            Log.d(TAG, "onLocationChanged: mLocation: " + mLastLocation.toString());
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 12.0f));
//            firstMove = false;
//        }

//        Log.d(TAG, "onConnected: Moving cam");
        prefManager.setCurrentLocation(String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude()));
//        currentLocationPoint = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
//        if (prefManager.isDoingRequest()) if (firstLocationToDriverRouting) {
//            showRoute();
//            firstLocationToDriverRouting = false;
//        }

//        if (!mMap.isMyLocationEnabled()) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            mMap.setMyLocationEnabled(true);
//            // Get the button view
//            View locationButton = ((View) findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
//
//            // and next place it, for exemple, on bottom right (as Google Maps app)
//            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
//            // position on right bottom
//            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
//            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//            int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
//            int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
//            rlp.setMargins(0, 0, right, bottom);
//        }

//        if(null!= mCurrentLocation)
//        Toast.makeText(this, "Updated: "+mCurrentLocation.getLatitude()+" "+mCurrentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

/*
    public String getTime() {
        SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return mDateFormat.format(new Date());
    }

    public void appendLog(String text, String filename)
    {
        File logFile = new File(filename);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
*/

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
            // TODO: Consider using a notification so that the request isn't bound to a certain activity
            //check
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

                EventBus.getDefault().post(new DriverActive(true));
//                prefManager
//                sendInactiveToLogout(1, prefManager.getCurrentLocation());
//                prefManager.setActive(true);
//                setUI();
//                setButtonsEnabledState();
            }
        });

    }
/*
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                Toast.makeText(this, "You can't receive requests unless you enable this permission\n" +
                                "if you want to receive requests, press the 'Go active' button below",
                        Toast.LENGTH_LONG).show();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }
*/

    private void activeNotification(Boolean enable) {

        if (enable) {

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);

//            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Active")
//                .setAutoCancel(true)
//                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(ACTIVE_NOTIFICATION_ID /* ID of notification */, notificationBuilder.build());
        }
        else
        {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(ACTIVE_NOTIFICATION_ID /* ID of notification */);

        }
    }
    @Override
    public void onDestroy() {
        // Turn off the request flag
        Log.d(TAG, "onDestroy");

        activeNotification(false);
        if(!prefManager.isExtermalLogout()) {
            sendInactiveToLogout(prefManager.getCurrentLocation());
        }
        else
            prefManager.setExternalLogout(false);

        this.mInProgress = false;

//        if (this.servicesAvailable && this.mGoogleApiClient != null) {
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
//                    prefManager.setActive(false);
                    activeNotification(false);
                } else if (response.code() == 401) {
                    Log.i(TAG, "onResponse: User not logged in");
                    prefManager.setIsLoggedIn(false);
                } else {
//                    clearHistoryEntries();
                    Log.i(TAG, "Unknown error occurred");
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                Log.i(TAG, getString(R.string.server_timeout));
                while(inactiveAttempts < 50 || !sentInactiveSuccessfully) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
                    sendInactiveToLogout(location);
                    inactiveAttempts++;
                }
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
//        Intent intent = new Intent(this, LocationReceiver.class);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if(!checkedLocation) {
            checkLocationSettings();
            checkedLocation = true;
        }
//        LocationServices.FusedLocationApi.requestLocationUpdates(this.mGoogleApiClient,
//                mLocationRequest, this); // This is the changed line.
//        appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Connected", Constants.LOG_FILE);

    }

    /*
 * Called by Location Services if the connection to the
 * location client drops because of an error.
 */
    @Override
    public void onConnectionSuspended(int i) {
        // Turn off the request flag
//        mInProgress = false;
//         Destroy the current location client
//        mGoogleApiClient = null;

        Log.d(TAG, "onConnectionSuspended");
        mGoogleApiClient.connect();

        // Display the connection status
        // Toast.makeText(this, DateFormat.getDateTimeInstance().format(new Date()) + ": Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
//        appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Disconnected", Constants.LOG_FILE);
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
}
/*
public class BackgroundLocationService extends Service {
    public BackgroundLocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
*/
