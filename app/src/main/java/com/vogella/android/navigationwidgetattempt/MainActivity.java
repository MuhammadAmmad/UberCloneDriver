package com.vogella.android.navigationwidgetattempt;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.PassengerArrived;
import com.Wisam.Events.PassengerCanceled;
import com.Wisam.POJO.StatusResponse;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;

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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.iid.FirebaseInstanceId;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
        , LocationListener {

    private static final long UPDATE_DURING_REQUEST = 10 * 1000; //10 seconds
    private static final long UPDATE_WHILE_IDLE = 30 * 60 * 1000;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2445;
    private static final int PERMISSION_REQUEST_LOCATION = 1432;
    private static final int PERMISSION_REQUEST_CLIENT_CONNECT = 365;
    private static final int LOCATION_REQUEST_CODE = 21314;
    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    public Location mLastLocation;
    private Location mCurrentLocation;
    private String mLastUpdateTime;

    private LatLng pickupPoint;
    private Marker pickupMarker;
    private LatLng destPoint;
    private Marker destMarker;
    private LatLng currentLocationPoint = null;
    private Marker currentLocationMarker;
    private Polyline pickupToDestRoute;
    private Polyline driverToPickupRoute;

    private int menuState = 0; //the user is signed out

    private static final int LOGIN_REQUEST_CODE = 10;
    private static final int ONGOING_REQUESTS_CODE = 50;
    private static driver driver = new driver();
    private static final String DUMMY_REQUEST_ID = "1243";
    private static final double DUMMY_PICKUP[] = {15.6023428, 32.5873593};
    private static final double DUMMY_DEST[] = {15.5551185, 32.5543017};
    private static final String DUMMY_PASSENGER_NAME = "John Green";
    private static final String DUMMY_PASSENGER_PHONE = "0123456789";
    private static final String DUMMY_STATUS = "on_the_way";
    private static final String DUMMY_NOTES = "Drive slowly";
    private static final String DUMMY_PRICE = "43";
    private static final String DUMMY_DEST_TEXT = "My home";
    private static final String DUMMY_PICKUP_TEXT = "My workplace";
    private static final String DUMMY_TIME = "06/11/2016 ; 15:45";
    private static request current_request = new request(DUMMY_REQUEST_ID, DUMMY_PICKUP, DUMMY_DEST,
            DUMMY_PASSENGER_NAME, DUMMY_PASSENGER_PHONE, DUMMY_TIME, DUMMY_PRICE, DUMMY_NOTES,
            DUMMY_STATUS, DUMMY_PICKUP_TEXT, DUMMY_DEST_TEXT);
    //    private static request current_request = new request();
    private static final int REQUEST_SUCCESS = 1;
    private static final int REQUEST_CANCELLED = 0;

    private static final String DUMMY_DRIVER_LOCATION = "15.6023428, 32.5873593";

    private RecyclerView previous_requests;
    private RecyclerView.Adapter RVadapter;
    private RecyclerView.LayoutManager layoutManager;
//    private Dialog myDialog;

    private static final String TAG = "MainActivity";
    private static final String GOOGLE_DIRECTIONS_API = "AIzaSyDpJmpRN0BxJ76X27K0NLTGs-gDHQtoxXQ";

    private PrefManager prefManager;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private boolean firstMove = true;
    private boolean mapIsReady;
    private boolean setWhenReady = false;
    private boolean firstLocationToDriverRouting;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.white));
        ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_active));
        ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
//        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
//        toolbar.setTitle(R.string.driver_active);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//        ((TextView) (navigationView.inflateHeaderView(R.layout.nav_header_main))
//                .findViewById(R.id.show_username)).setText(driver.getUsername());
        navigationView.setNavigationItemSelectedListener(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        String token = FirebaseInstanceId.getInstance().getToken();

        // Log and toast
        String msg = getString(R.string.msg_token_fmt, token);
        Log.d(TAG, msg);

        final TextView changeDriverStatus = (TextView) findViewById(R.id.change_driver_status);
        changeDriverStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (changeDriverStatus.getText().toString().equals(getString(R.string.go_inactive))) {
//                if (prefManager.isActive()) {
                    Log.d(TAG,"changeDriverStatus button pressed. Attempting to change from avaialble to away");
                    ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_active);
                    ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.white));
                    ((TextView) findViewById(R.id.change_driver_status)).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//                    toolbar.setTitle(R.string.driver_inactive);
//                    toolbar.setTitleTextColor(getResources().getColor(R.color.colorAccent));
                    ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_inactive));
                    ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorAccent));
//                    setSupportActionBar(toolbar);
                    prefManager.setActive(false);
                    sendActive(0, prefManager.getCurrentLocation());
                } else if (changeDriverStatus.getText().toString().equals(getString(R.string.go_active))) {
//                } else{
                    Log.d(TAG,"changeDriverStatus button pressed. Attempting to change from away to available");
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG,"changeDriverStatus button pressed. Permissions were not given at the time");
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_CLIENT_CONNECT);
                    } else {
                        if(mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
                            Log.d(TAG, "changeDriverStatus button pressed. mGoogleApiClient isConnected/Connecting. Calling initializeLocation");
                            initializeLocation();
                        }
                        else{
                            Log.d(TAG, "changeDriverStatus button pressed. mGoogleApiClient isConnected/Connecting. Calling initializeLocation");
                            mGoogleApiClient.connect();
                        }
                    }
//                    changeDriverStatus.setText(R.string.driver_active);
//                    prefManager.setActive(true);
//                    sendActive(1, prefManager.getCurrentLocation());
                }
            }
        });
//        boolean cancel = true;
//        setCurrentRequestOnClickListeners(cancel);
//        previous_requests = (RecyclerView) findViewById(R.id.past_requests);
//        previous_requests.setHasFixedSize(true);
//        layoutManager = new LinearLayoutManager(this);
//        previous_requests.setLayoutManager(layoutManager);

        prefManager = new PrefManager(this);
        // ==================== To get location ================

        // Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Request permissions



    }

    private void setCurrentRequestOnClickListeners() {
            Button cancelRequest = (Button) findViewById(R.id.cancel_request);
            cancelRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alerBuilder = new AlertDialog.Builder(MainActivity.this);
                    alerBuilder.setMessage(getString(R.string.cancel_current_request_message));
                    alerBuilder.setPositiveButton(getString(R.string.cancel_current_request), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendCancel(current_request.getRequest_id());
                        }
                    });
                    alerBuilder.setNegativeButton(getString(R.string.dont_cancel_current_request), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    alerBuilder.show();
                }
            });
        final Button nextState;
            nextState = (Button) findViewById(R.id.next_state);
            nextState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView current = (TextView) findViewById(R.id.current_status);
                    current.setText(nextState.getText().toString());
                    current_request.nextStatus();
                    sendStatus(current_request.getRequest_id(), current_request.getStatus());
                    if (current_request.getStatus().equals("passenger_onboard")) {
                        setUI(UI_STATE.DOINGREQUEST);
                    } else if (current_request.getStatus().equals("completed")) {
                        endRequest(REQUEST_SUCCESS);
                        setUI(UI_STATE.SIMPLE);
                    } else

                        nextState.setText(current_request.getDisplayStatus(current_request.getNextStatus(), MainActivity.this));
                }
            });
        TextView current;
            current = (TextView) findViewById(R.id.current_status);
        current.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.current_request_view).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.cr_time)).setText(current_request.getTime());
                ((TextView) findViewById(R.id.cr_passenger_name)).setText(current_request.getPassenger_name());
                ((TextView) findViewById(R.id.cr_passenger_phone)).setText(current_request.getPassenger_phone());
                ((TextView) findViewById(R.id.cr_price)).setText(current_request.getPrice());
                ((TextView) findViewById(R.id.cr_status)).setText(current_request.getDisplayStatus(current_request.getStatus(), MainActivity.this));
                ((TextView) findViewById(R.id.cr_notes)).setText(current_request.getNotes());
                ((TextView) findViewById(R.id.cr_pickup)).setText(current_request.getPickupText());
                ((TextView) findViewById(R.id.cr_dest)).setText(current_request.getDestText());
                ((TextView) findViewById(R.id.cr_time)).setText(current_request.getTime());

                Button hide_request = (Button) findViewById(R.id.cr_close);
                hide_request.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        findViewById(R.id.current_request_view).setVisibility(View.INVISIBLE);
                    }
                });
                TextView passengerPhone = (TextView) findViewById(R.id.cr_passenger_phone);
                passengerPhone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder alerBuilder = new AlertDialog.Builder(MainActivity.this);
                        alerBuilder.setMessage(getString(R.string.call_passenger_message) + current_request.getPassenger_phone() + "?");
                        alerBuilder.setPositiveButton(getString(R.string.call_passenger), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:".concat(current_request.getPassenger_phone())));
                                startActivity(intent);
                            }
                        });
                        alerBuilder.setNegativeButton(getString(R.string.dont_call), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        alerBuilder.show();
                    }
                });

            }
        });
    }

        private void initializeLocation() {
        // Location Request
        mLocationRequest = new LocationRequest();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(1000);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(500);


        // Check device location settings
        LocationSettingsRequest.Builder locationSettingsReqBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        locationSettingsReqBuilder.build());
        Log.d(TAG,"initializeLocation: checking location settings .. ");
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
//                final LocationSettingsStates = result.getLocationSettingsStates();
                Log.d(TAG,"LocationSettingsResult called onResult");
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        Log.d(TAG,"LocationSettingsStatusCodes.SUCCESS");
                        ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_inactive);
                        ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.colorAccent));
                        ((TextView) findViewById(R.id.change_driver_status)).setBackgroundColor(getResources().getColor(R.color.white));
//                        toolbar.setTitle(R.string.driver_active);
//                        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));

                        ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_active));
                        ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
//                        setSupportActionBar(toolbar);
                        prefManager.setActive(true);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        Log.d(TAG,"LocationSettingsStatusCodes.RESOLUTION_REQUIRED");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    LOCATION_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        Log.d(TAG,"LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE");

                        break;
                }
            }
        });

        // Set UI
//        setUI(UI_STATE.CONFIRM_PICKUP);
//
//        ride = new Ride();
//
//
//        Intent intent = new Intent(this, RideRequestService.class);
//        startService(intent);
//        // getDrivers
//        ride.getDrivers(this, KHARTOUM_CORDS);
//        mGoogleApiClient.connect();
    }

    void endRequest(int res) {
//        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
//        linearLayout.setVisibility(View.INVISIBLE);
        if (res == REQUEST_SUCCESS)
            Toast.makeText(MainActivity.this, R.string.current_request_completed,
                    Toast.LENGTH_LONG).show();
        else if (res == REQUEST_CANCELLED)
            Toast.makeText(MainActivity.this, R.string.current_request_cancelled,
                    Toast.LENGTH_LONG).show();
        OngoingRequestsActivity.removeRequest(current_request.getRequest_id());
//        current_request = new request();
//        if (pickupMarker != null) {
//            pickupMarker.remove();
//        }
//        if (destMarker != null) {
//            destMarker.remove();
//        }
//        if (currentLocationMarker != null) {
//            currentLocationMarker.remove();
//        }
//        if (pickupToDestRoute != null) {
//            pickupToDestRoute.remove();
//        }
//        if (driverToPickupRoute != null) {
//            driverToPickupRoute.remove();
//        }
//        prefManager.setRequestId("");
        prefManager.setDoingRequest(false);
        Intent locationIntent = new Intent(getApplicationContext(), UpdateLocation_Active.class);
        locationIntent.putExtra("alarmType", "location");
        alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, UPDATE_WHILE_IDLE, alarmIntent);
//        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime(), UPDATE_WHILE_IDLE,
//                alarmIntent);
    }

    private void sendStatus(String request_id, final String status) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.updating_request_status));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();


        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.status("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                request_id, status);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if(progress.isShowing())progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
//                    Toast.makeText(MainActivity.this, "The request status has been updated successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The status has been sent successfully");
                    if(status.equals("on_the_way")){
                        prefManager.isDoingRequest();
                        setUI(UI_STATE.DOINGREQUEST);
                    }
                } else if (response.code() == 401) {
                    Toast.makeText(MainActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(intent);
                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(MainActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if(progress.isShowing())progress.dismiss();
                Toast.makeText(MainActivity.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.d(TAG, getString(R.string.server_timeout));
            }
        });
    }

    private void sendActive(int active, final String location) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.updating_driver_status));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.active("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                active, location);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if(progress.isShowing()) progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
                    Toast.makeText(MainActivity.this, R.string.driver_status_changed_successfully, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The driver status has been changed successfully");
                } else if (response.code() == 401) {
                    Toast.makeText(MainActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onResponse: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(intent);
                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
                    Log.i(TAG, "Unknown error occurred");
                    Toast.makeText(MainActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if(progress.isShowing()) progress.dismiss();
                Toast.makeText(MainActivity.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.i(TAG, getString(R.string.server_timeout));
            }
        });
    }

    private void sendCancel(final String request_id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.cancelling_request));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();


        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.cancel("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                request_id);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if(progress.isShowing()) progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
                    endRequest(REQUEST_CANCELLED);
                    setUI(UI_STATE.SIMPLE);
                    Toast.makeText(MainActivity.this, R.string.request_cancelled_successfully, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The request has been cancelled successfully");
                } else if (response.code() == 401) {
                    Toast.makeText(MainActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onResume: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(intent);
                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(MainActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if(progress.isShowing()) progress.dismiss();
                Toast.makeText(MainActivity.this,R.string.server_timeout , Toast.LENGTH_SHORT).show();
                Log.i(TAG, getString(R.string.server_timeout));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ONGOING_REQUESTS_CODE && resultCode == RESULT_OK) {
//            Toast.makeText(this,data.getExtras().getString("passenger_name"), Toast.LENGTH_LONG).show();
            if (data.hasExtra("request_id")) {
                //set the data
                //TODO: check if you are already doing a request
                request temp = new request();
                temp.setPassenger_name(data.getExtras().getString("passenger_name"));
                temp.setPassenger_phone(data.getExtras().getString("passenger_phone"));
                temp.setStatus(data.getExtras().getString("status"));
                double pickup[] = new double[2];
                pickup[0] = data.getExtras().getDouble("pickup_longitude");
                pickup[1] = data.getExtras().getDouble("pickup_latitude");
                temp.setPickup(pickup);
                double dest[] = new double[2];
                dest[0] = data.getExtras().getDouble("dest_longitude");
                dest[1] = data.getExtras().getDouble("dest_latitude");
                temp.setDest(dest);
                temp.setTime(data.getExtras().getString("time"));
                temp.setNotes(data.getExtras().getString("notes"));
                temp.setPrice(data.getExtras().getString("price"));
                temp.setDestText(data.getStringExtra("dest_text"));
                temp.setPickupText(data.getStringExtra("pickup_text"));
                temp.setRequest_id(data.getStringExtra("request_id"));
                prefManager.setRequest(temp);
//                current_request = new request();
//                current_request.setPassenger_name(data.getExtras().getString("passenger_name"));
//                current_request.setPassenger_phone(data.getExtras().getString("passenger_phone"));
//                current_request.setStatus(data.getExtras().getString("status"));
//                double pickup[] = new double[2];
//                pickup[0] = data.getExtras().getDouble("pickup_longitude");
//                pickup[1] = data.getExtras().getDouble("pickup_latitude");
//                current_request.setPickup(pickup);
//                double dest[] = new double[2];
//                dest[0] = data.getExtras().getDouble("dest_longitude");
//                dest[1] = data.getExtras().getDouble("dest_latitude");
//                current_request.setDest(dest);
//                current_request.setTime(data.getExtras().getString("time"));
//                current_request.setNotes(data.getExtras().getString("notes"));
//                current_request.setPrice(data.getExtras().getString("price"));
//                current_request.setRequest_id(data.getExtras().getString("request_id"));
                startRequest();
//                Gson gson = new Gson();
//                String json = gson.toJson(current_request);
//                prefManager.editor.putString("current_request", json);
//                prefManager.setDoingRequest(true);


            }
        }
        if (requestCode == LOCATION_REQUEST_CODE ) {
            if (resultCode != RESULT_OK) {
                Log.d(TAG, "onActivityResult: the user didn't enable location");
                ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_active);
                ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.white));
                ((TextView) findViewById(R.id.change_driver_status)).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//                toolbar.setTitle(R.string.driver_inactive);
//                toolbar.setTitleTextColor(getResources().getColor(R.color.colorAccent));
//                setSupportActionBar(toolbar);

                ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_inactive));
                ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorAccent));
                prefManager.setActive(false);
            } else{
                Log.d(TAG, "onActivityResult: the user enabled location");
                ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_inactive);
                ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.colorAccent));
                ((TextView) findViewById(R.id.change_driver_status)).setBackgroundColor(getResources().getColor(R.color.white));
//                toolbar.setTitle(R.string.driver_active);
//                toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
//                setSupportActionBar(toolbar);

                ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_active));
                ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
                prefManager.setActive(true);
            }
        }
    }

    private void startRequest() {
        //                pickupPoint = new LatLng(pickup[0], pickup[1]);


//        prefManager.setRequestId(current_request.getRequest_id());
        Intent locationIntent = new Intent(getApplicationContext(), UpdateLocation_Active.class);
        locationIntent.putExtra("alarmType", "location");
        alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, UPDATE_DURING_REQUEST, alarmIntent);

//        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime(), UPDATE_DURING_REQUEST,
//                alarmIntent);
        current_request = prefManager.getRequest();

        sendStatus(current_request.getRequest_id(),current_request.getStatus());
//        while(no_response);
//        if(onTheWay)
//            prefManager.setDoingRequest(true);

//        pickupPoint = new LatLng(current_request.getPickup()[0], current_request.getPickup()[1]);
//        destPoint = new LatLng(current_request.getDest()[0], current_request.getDest()[1]);
//
//
//        // Setting marker
//        if (pickupMarker != null) {
//            pickupMarker.remove();
//        }
//        pickupMarker = mMap.addMarker(new MarkerOptions()
//                .position(pickupPoint)
//                .title("Pickup")
//                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.start_loc_smaller))
//        );
//
//
//        // Setting marker
//        if (destMarker != null) {
//            destMarker.remove();
//        }
//
//        destMarker = mMap.addMarker(new MarkerOptions()
//                .position(destPoint)
//                .title("Destination")
//                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.stop_loc_smaller))
//        );
//
//
//        showRoute();
//
//
//        // For zooming automatically to the location of the marker
//        CameraPosition cameraPosition = new CameraPosition.Builder().target(pickupPoint).zoom(12).build();
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//
//        //set values for the different views
//        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
//        Button nextState = (Button) findViewById(R.id.next_state);
//        TextView current = (TextView) findViewById(R.id.current_status);
//        current.setText(current_request.getDisplayStatus(current_request.getStatus()));
//        String temp = current_request.getStatus();
//        current_request.nextStatus();
//        nextState.setText(current_request.getDisplayStatus(current_request.getStatus()));
//        current_request.setStatus(temp);
//        linearLayout.setVisibility(View.VISIBLE);
    }

    public enum UI_STATE{
        SIMPLE,
        DOINGREQUEST,
        STATUS_MESSAGE
    }

    public void setUI(UI_STATE state){
        switch (state){
            case SIMPLE:
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
                linearLayout.setVisibility(View.INVISIBLE);
                if (pickupMarker != null) {
                    pickupMarker.remove();
                }
                if (destMarker != null) {
                    destMarker.remove();
                }
                if (currentLocationMarker != null) {
                    currentLocationMarker.remove();
                }
                if (pickupToDestRoute != null) {
                    pickupToDestRoute.remove();
                }
                if (driverToPickupRoute != null) {
                    driverToPickupRoute.remove();
                }
                break;
            case DOINGREQUEST:
                pickupPoint = new LatLng(current_request.getPickup()[0], current_request.getPickup()[1]);
                destPoint = new LatLng(current_request.getDest()[0], current_request.getDest()[1]);

                if(mapIsReady)
                    setMarkers();
                else
                    setWhenReady = true;


                //set values for the different views
                Button nextState;
                TextView current;
                linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
                nextState = (Button) findViewById(R.id.next_state);
                current = (TextView) findViewById(R.id.current_status);
                current.setText(current_request.getDisplayStatus(current_request.getStatus(), MainActivity.this));
                nextState.setText(current_request.getDisplayStatus(current_request.getNextStatus(), MainActivity.this));
                linearLayout.setVisibility(View.VISIBLE);
                if(current_request.getStatus().equals("passenger_onboard") ||
                        current_request.getStatus().equals("arrived_dest")||
                        current_request.getStatus().equals("completed")) {
                    ((Button) findViewById(R.id.cancel_request)).setVisibility(View.GONE);
                    ((View) findViewById(R.id.current_request_separator)).setVisibility(View.GONE);
                }
                else {
                    ((Button) findViewById(R.id.cancel_request)).setVisibility(View.VISIBLE);
                    ((View) findViewById(R.id.current_request_separator)).setVisibility(View.VISIBLE);
                }

                setCurrentRequestOnClickListeners();
        }
    }

    private void setMarkers() {
        Log.d(TAG,"setMarkers has been called");
        // Setting marker
        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        pickupMarker = mMap.addMarker(new MarkerOptions()
                .position(pickupPoint)
                .title("Pickup")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.start_loc_smaller))
        );


        // Setting marker
        if (destMarker != null) {
            destMarker.remove();
        }

        destMarker = mMap.addMarker(new MarkerOptions()
                .position(destPoint)
                .title("Destination")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.stop_loc_smaller))
        );


        showRoute();


        // For zooming automatically to the location of the marker
        CameraPosition cameraPosition = new CameraPosition.Builder().target(pickupPoint).zoom(12).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(prefManager.isDoingRequest()){
            prefManager.setRequest(current_request);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!prefManager.isLoggedIn()){
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            MainActivity.this.startActivity(intent);
            MainActivity.super.finish();
        }
        //get the driver information
        driver = prefManager.getDriver();
//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        //if the driver is active, enable the location and schedule location and active requests.
        if(prefManager.isActive()){
            //ensure location is enabled
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_LOCATION);
            } else {
                initializeLocation();
            }
            //setup location alarm
            alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            Intent locationIntent = new Intent(getApplicationContext(), UpdateLocation_Active.class);
            locationIntent.putExtra("alarmType", "location");
            boolean locationFlag = (PendingIntent.getBroadcast(getApplicationContext(), 0, locationIntent, PendingIntent.FLAG_NO_CREATE) == null);
/*Register alarm if not registered already*/
//            if(locationFlag){
                alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

/* Setting alarm for every one hour from the current time.*/
                int intervalTimeMillis;
                if (prefManager.isDoingRequest())
                    intervalTimeMillis = 10 * 1000;  // 10 seconds
                else
                    intervalTimeMillis = 5 * 60 * 1000;//5 * 60 * 1000; // 5 minutes
                alarmMgr.set(AlarmManager.RTC_WAKEUP, intervalTimeMillis, alarmIntent);
//             alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                        SystemClock.elapsedRealtime(), intervalTimeMillis,
//                        alarmIntent);
//            }
            Intent activeIntent = new Intent(getApplicationContext(), UpdateLocation_Active.class);
            activeIntent.putExtra("alarmType", "active");
            boolean activeFlag = (PendingIntent.getBroadcast(getApplicationContext(), 67769, activeIntent, PendingIntent.FLAG_NO_CREATE) == null);
/*Register alarm if not registered already*/
//            if(activeFlag){
                alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 67769, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

/* Setting alarm for every one hour from the current time.*/
//                int intervalTimeMillis;
                    intervalTimeMillis = 30 * 1000;//30 * 1000; // 30 seconds
                alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, intervalTimeMillis, alarmIntent);
//                alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                        SystemClock.elapsedRealtime(), intervalTimeMillis,
//                        alarmIntent);
////            }

        }

        //Setup the UI based on whether there is a current request
        if(prefManager.isDoingRequest()){
            current_request = prefManager.getRequest();
            if(current_request.getStatus().equals("completed")){
                Log.d(TAG,"The passenger marked the request as complete");
                endRequest(REQUEST_SUCCESS);
                setUI(UI_STATE.SIMPLE);
                current_request = new request();
            }
            else
                if(current_request.getStatus().equals("canceled")){
                Log.d(TAG,"The passenger canceled the request");
                endRequest(REQUEST_CANCELLED);
                    setUI(UI_STATE.SIMPLE);
                current_request = new request();
            }
            else {
                    Log.d(TAG,"The driver is doing a request");
//                    current_request = prefManager.getRequest();
                    setUI(UI_STATE.DOINGREQUEST);
                }
        }
        else {
            setUI(UI_STATE.SIMPLE);
        }

//        prefManager.setCurrentLocation(DUMMY_DRIVER_LOCATION);

//        if(prefManager.getRequestStatus().equals("completed")){
//            Log.d(TAG,"The request has been completed");
//            endRequest(REQUEST_SUCCESS);
//        }
//        if(prefManager.getRequestStatus().equals("canceled")){
//            Log.d(TAG,"The request has been cancelled");
//            endRequest(REQUEST_CANCELLED);
//        }

//        if(prefManager.isDoingRequest()){
//            Gson gson = new Gson();
//            String json = prefManager.pref.getString("current_request","");
//            current_request = gson.fromJson(json, request.class);
//            startRequest();
//        }
    }


    @Override
    public void onStart() {
//        if(prefManager.isActive()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_CLIENT_CONNECT);
            } else {
                mGoogleApiClient.connect();
            }
//        }
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassengerCancelled(PassengerCanceled event) {
        Log.d(TAG,"onPassengerCanceled has been invoked");
        endRequest(REQUEST_CANCELLED);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverLoggedout(DriverLoggedout event) {
        Log.d(TAG,"onDriverLoggedout has been invoked");
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        MainActivity.this.startActivity(intent);
        MainActivity.super.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassngerArrived(PassengerArrived event) {
        Log.d(TAG,"onPassengerArrived has been invoked");
        endRequest(REQUEST_SUCCESS);
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = new Intent(this, FCMRequest.class);
        intent.putExtra("request_id", "13213");
        intent.putExtra("price", "33");
        intent.putExtra("pickup", "15.6023428, 32.5873593");
        intent.putExtra("dest", "15.5023428, 32.3873593");
//        intent.putExtra("time","25/10/16 4:33");
        //intent.putExtra("time","1479935269");
        intent.putExtra("time", "now");
        intent.putExtra("passenger_name", "George Washington");
        intent.putExtra("passenger_phone", "0999999999");
        intent.putExtra("notes", "Don't smoke! Don't drive while texting!");
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);

        } else if (id == R.id.current_requests) {
            Intent intent = new Intent(this, OngoingRequestsActivity.class);
            startActivityForResult(intent, ONGOING_REQUESTS_CODE);

        } else if (id == R.id.profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);

        } else if (id == R.id.about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);

        } else if (id == R.id.sign_out) {
            prefManager.setIsLoggedIn(false);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        mapIsReady = true;
        LatLng khartoum = new LatLng(15.5838046, 32.5543825);
//        mMap.addMarker(new MarkerOptions().position(khartoum).title("Marker in Khartoum"));
        if(firstMove)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(khartoum, 12));

        if(setWhenReady){
            setWhenReady = false;
            setMarkers();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setPadding(0, 150, 0, 0);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION },
                        PERMISSION_REQUEST_LOCATION);
            }
            else {
//                initializeLocation();

            }
        } else if(requestCode == PERMISSION_REQUEST_CLIENT_CONNECT){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION },
                        PERMISSION_REQUEST_LOCATION);
            }
            else {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.d(TAG, "onConnected: I am connected");
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.d(TAG, "onConnected: Connected");
//        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                mGoogleApiClient);
//        if (mLastLocation != null) {
//            mCurrentLocation = mLastLocation;
//            Toast.makeText(this, "Connected GPlServices "+mLastLocation.getLatitude()+" "+mLastLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            // Get drivers
//            ride.getDrivers(MapsActivity.this, new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
//        }
//        else {
//            Toast.makeText(this, "Sorry, it's null", Toast.LENGTH_SHORT).show();
//
//        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"onConnectionSuspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG,"onConnectionFailed");

    }
    @Override
    public void onLocationChanged(Location location) {

        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.d(TAG, "onLocationChanged: mLocation: "+mCurrentLocation.toString());
        if (firstMove && mLastLocation != null){
            Log.d(TAG, "onLocationChanged: Moving cam");
            Log.d(TAG, "onLocationChanged: mLocation: "+mLastLocation.toString());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 12.0f));
            firstMove = false;
        }
//        Log.d(TAG, "onConnected: Moving cam");
        prefManager.setCurrentLocation(String.valueOf(mCurrentLocation.getLatitude()) + "," + String.valueOf(mCurrentLocation.getLongitude()));
        currentLocationPoint = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
        if(prefManager.isDoingRequest()) if(firstLocationToDriverRouting) {showRoute(); firstLocationToDriverRouting = false;}
//        if(null!= mCurrentLocation)
//        Toast.makeText(this, "Updated: "+mCurrentLocation.getLatitude()+" "+mCurrentLocation.getLongitude(), Toast.LENGTH_SHORT).show();

    }

    private void showRoute() {
        Log.d(TAG, "showRoute: Called");

//        if (routePolyline != null) {
//            routePolyline.remove();
//        }

        GoogleDirection.withServerKey(GOOGLE_DIRECTIONS_API)
                .from(pickupPoint)
                .to(destPoint)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        // Do something here
//                        Toast.makeText(MainActivity.this, "Route successfully computed ", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "showRoute:Pickup to Destination Route successfully computed ");

                        if (direction.isOK()) {
                            // Do
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);

                            // Distance info
                            Info distanceInfo = leg.getDistance();
                            Info durationInfo = leg.getDuration();
                            String distance = distanceInfo.getText();
                            String duration = durationInfo.getText();

                            ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                            PolylineOptions polylineOptions = DirectionConverter.createPolyline(MainActivity.this, directionPositionList, 5, getResources().getColor(R.color.colorPrimary));
                            if (pickupToDestRoute != null) {
                                pickupToDestRoute.remove();
                            }
                            pickupToDestRoute = mMap.addPolyline(polylineOptions);

                        }

                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something here
                        Toast.makeText(MainActivity.this, R.string.pickup_to_dest_route_failed, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "showRoute:Pickup to Destination Route Failed ");
                    }
                });
        if (currentLocationPoint != null) {
            GoogleDirection.withServerKey(GOOGLE_DIRECTIONS_API)
//                .from(mCurrentLocation.)
                    .from(currentLocationPoint)
                    .to(pickupPoint)
                    .execute(new DirectionCallback() {
                        @Override
                        public void onDirectionSuccess(Direction direction, String rawBody) {
                            // Do something here
//                        Toast.makeText(MainActivity.this, "Route successfully computed ", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "showRoute:Driver to Pickup Route successfully computed ");

                            if (direction.isOK()) {
                                // Do
                                Route route = direction.getRouteList().get(0);
                                Leg leg = route.getLegList().get(0);

                                // Distance info
                                Info distanceInfo = leg.getDistance();
                                Info durationInfo = leg.getDuration();
                                String distance = distanceInfo.getText();
                                String duration = durationInfo.getText();

                                ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                                PolylineOptions polylineOptions = DirectionConverter.createPolyline(MainActivity.this, directionPositionList, 5, getResources().getColor(R.color.colorAccent));
                                if (driverToPickupRoute != null) {
                                    driverToPickupRoute.remove();
                                }
                                driverToPickupRoute = mMap.addPolyline(polylineOptions);

                            }

                        }

                        @Override
                        public void onDirectionFailure(Throwable t) {
                            // Do something here
                            Toast.makeText(MainActivity.this, R.string.driver_to_pickup_route_failed, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "showRoute:Driver to Pickup Route Failed ");
                        }
                    });
        }
    }

}
