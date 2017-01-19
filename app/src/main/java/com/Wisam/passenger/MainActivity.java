package com.Wisam.passenger;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.ChangeActiveUpdateInterval;
import com.Wisam.Events.DriverActive;
import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.LocationUpdated;
import com.Wisam.Events.PassengerArrived;
import com.Wisam.Events.PassengerCanceled;
import com.Wisam.Events.UnbindBackgroundLocationService;
import com.Wisam.POJO.StatusResponse;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;

import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.Wisam.passenger.BackgroundLocationService.ACCESS_FINE_LOCATION_CODE;
import static com.Wisam.passenger.BackgroundLocationService.checkedLocation;
import static com.Wisam.passenger.BackgroundLocationService.permissionIsRequested;
import static com.Wisam.passenger.RestServiceConstants.REQUEST_CHECK_SETTINGS;
import static com.Wisam.passenger.RestServiceConstants.goActiveID;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback//,
{

    protected static final int UPDATE_DURING_REQUEST = 10 * 1000; //10 seconds
    protected static final int UPDATE_WHILE_IDLE = 2 * 60 * 1000;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2445;
    private static final int PERMISSION_REQUEST_LOCATION = 1432;
    private static final int PERMISSION_REQUEST_CLIENT_CONNECT = 365;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 2000;
    static final int ACTIVE_NOTIFICATION_ID = 1024;
    private static final int RESENDING_ATTEMPTS_OVERALL_DELAY = 5 * 60 * 1000;
    private static final int FINISH_PARENT = 7;
    private GoogleMap mMap;

    private ProgressDialog progress;


    public Location mLastLocation;
    private Location mCurrentLocation;
    private String mLastUpdateTime;

    private LatLng pickupPoint;
    private Marker pickupMarker;
    private LatLng destPoint;
    private Marker destMarker;
    private static LatLng currentLocationPoint = null;
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
    private static final int REQUEST_SUCCESS = 1;
    private static final int REQUEST_CANCELLED = 0;

    private static final String DUMMY_DRIVER_LOCATION = "15.6023428, 32.5873593";

    private RecyclerView previous_requests;
    private RecyclerView.Adapter RVadapter;
    private RecyclerView.LayoutManager layoutManager;

    private static final String TAG = "MainActivity";
    private static final String GOOGLE_DIRECTIONS_API = "AIzaSyDpJmpRN0BxJ76X27K0NLTGs-gDHQtoxXQ";

    private PrefManager prefManager;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private boolean firstMove = true;
    private boolean mapIsReady = false;
    private boolean setWhenReady = false;
    private boolean firstLocationToDriverRouting;
    private Toolbar toolbar;
    private boolean wasActive;

    protected static MainActivity context;

    private static BackgroundLocationService backgroundLocationService;
    protected static ServiceConnection mConnection;



    protected static Intent blsIntent;
    protected static boolean mIsBound = false;

    private CameraUpdate cu;
    private boolean zoomToRoute = false;
    private LatLngBounds bounds;
    private boolean createdFromNewRequest;
    private Handler routingHandler;
    private Runnable routingRunnable;
    private Handler resendActiveHandler;
    private long resendFailedRequestDelay = 10 * 1000;
    private int resendActiveAttempts = 0;
    private Handler resendStatusHandler;
    private int resendStatusAttempts = 0;
    private boolean routeCancelled;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.white));
        ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_active));
        ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

//        goActive = false;
        DataHolder.save(goActiveID, MainActivity.this);

        final TextView changeDriverStatus = (TextView) findViewById(R.id.change_driver_status);
        changeDriverStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (changeDriverStatus.getText().toString().equals(getString(R.string.go_inactive))) {
                    Log.d(TAG, "changeDriverStatus button pressed. Attempting to change from avaialble to away");
                    prefManager.setActive(false);
                    prefManager.setGoActive(false);
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    if (mIsBound) {
//                        backgroundLocationService.setGoActive(false);
                        getApplicationContext().unbindService(mConnection);
                        mIsBound = false;
                    }
                    if (blsIntent != null)
                        stopService(blsIntent);
//                    if(goActive)
//                        goActive = false;
                    sendActive(0, prefManager.getCurrentLocation());
                    setUI();
                } else if (changeDriverStatus.getText().toString().equals(getString(R.string.go_active))) {
                    Log.d(TAG, "changeDriverStatus button pressed. Attempting to change from away to available");
//                    goActive = true;
//                    if(mIsBound){
//                        backgroundLocationService.setGoActive(true);
//                    })
                    if(resendActiveHandler != null)
                        resendActiveHandler.removeCallbacksAndMessages(null);
                    resendActiveAttempts = 0;
                    prefManager.setGoActive(true);
                    startAndBindLocationService();
                    setUI();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        String token = FirebaseInstanceId.getInstance().getToken();

        String msg = getString(R.string.msg_token_fmt, token);
        Log.d(TAG, msg);

        prefManager = new PrefManager(this);

        Locale arabicLocale = new Locale("ar");
        Locale englishLocale = new Locale("en");
        Locale desiredLocale;
        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;
        if (savedLocale.equals(arabicLocale))
            desiredLocale = englishLocale;
        else
            desiredLocale = arabicLocale;
        conf.locale = desiredLocale; // whatever you want here
        res.updateConfiguration(conf, null); // second arg null means don't change

// retrieve resources from desired locale
        String otherLanguage = res.getString(R.string.Language);

// restore original locale
        conf.locale = savedLocale;
        res.updateConfiguration(conf, null);

        if (prefManager.usingOtherLanguage())
            navigationView.getMenu().getItem(5).setTitle(otherLanguage);


        createdFromNewRequest = false;

        getCurrentRequest(getIntent());

        context = this;

        blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);

        if (prefManager.isActive()) {

            startAndBindLocationService();

        }


    }

    private void startAndBindLocationService() {
        startService(blsIntent);


        mConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  Because we have bound to a explicit
                // service that we know is running in our own process, we can
                // cast its IBinder to a concrete class and directly access it.
                backgroundLocationService = ((BackgroundLocationService.LocalBinder) service).getServerInstance();
                if (!checkedLocation) {
                    backgroundLocationService.setActivityWeakReference(MainActivity.this);
                    backgroundLocationService.checkLocationSettings();
                    checkedLocation = true;
                }
                mIsBound = true;

                setUI();

                // Tell the user about this for our demo.
                //            Toast.makeText(Binding.this, R.string.local_service_connected,
                //                    Toast.LENGTH_SHORT).show();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                // Because it is running in our same process, we should never
                // see this happen.
                backgroundLocationService = null;
                mIsBound = false;

            }
        };

        getApplicationContext().bindService(blsIntent, mConnection, BIND_IMPORTANT);

    }

    private void setOnClickListeners() {

        TextView cancelRequest = (TextView) findViewById(R.id.cancel_request);
        cancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (current_request.getStatus().equals("passenger_onboard") ||
                        current_request.getStatus().equals("arrived_dest") ||
                        current_request.getStatus().equals("completed")) {
                    findViewById(R.id.current_request_view).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.cr_price)).setText(current_request.getPrice());
                    ((TextView) findViewById(R.id.cr_notes)).setText(current_request.getNotes());

                    TextView hide_request = (TextView) findViewById(R.id.cr_close);
                    hide_request.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            findViewById(R.id.current_request_view).setVisibility(View.INVISIBLE);
                        }
                    });


                } else {
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

            }
        });

        final TextView nextState;
        nextState = (TextView) findViewById(R.id.next_state);
        nextState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendStatus(current_request.getRequest_id(), current_request.getNextStatus());
            }
        });

        ImageView details;
        details = (ImageView) findViewById(R.id.view_details);
        details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.current_request_view).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.cr_price)).setText(current_request.getPrice());
                ((TextView) findViewById(R.id.cr_notes)).setText(current_request.getNotes());
                TextView hide_request = (TextView) findViewById(R.id.cr_close);
                hide_request.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        findViewById(R.id.current_request_view).setVisibility(View.INVISIBLE);
                    }
                });
            }
        });

        ImageView passengerPhone = (ImageView) findViewById(R.id.cr_passenger_phone);
        passengerPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alerBuilder = new AlertDialog.Builder(MainActivity.this);
                alerBuilder.setMessage(getString(R.string.call_passenger_message));
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

        LinearLayout navigation = (LinearLayout) findViewById(R.id.nav_button);
        navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri;
                if (current_request.getStatus().equals("passenger_onboard") ||
                        current_request.getStatus().equals("arrived_dest") ||
                        current_request.getStatus().equals("completed")) {

                    gmmIntentUri = Uri.parse("google.navigation:q=" + String.valueOf(destPoint.latitude)
                            + "," + String.valueOf(destPoint.longitude));

                } else {
                    gmmIntentUri = Uri.parse("google.navigation:q=" + String.valueOf(pickupPoint.latitude)
                            + "," + String.valueOf(pickupPoint.longitude));
                }
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);

            }
        });

    }


    void endRequest(int res) {
        if (res == REQUEST_SUCCESS)
            Toast.makeText(MainActivity.this, R.string.current_request_completed,
                    Toast.LENGTH_LONG).show();
        else if (res == REQUEST_CANCELLED)
            Toast.makeText(MainActivity.this, R.string.current_request_cancelled,
                    Toast.LENGTH_LONG).show();
        prefManager.setDoingRequest(false);


        EventBus.getDefault().post(new ChangeActiveUpdateInterval());


    }

    private void sendStatus(final String request_id, final String status) {
        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(MainActivity.this))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        if (!status.equals("on_the_way")) {

            progress = new ProgressDialog(this);
            progress.setMessage(getString(R.string.updating_request_status));
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();
        }

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.status("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                request_id, status);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if (!status.equals("on_the_way"))
                    if (!MainActivity.this.isFinishing() && progress != null && progress.isShowing())
                        progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
                    Log.d(TAG, "The status has been sent successfully");
                    if (status.equals("on_the_way")) {
                        if(resendStatusHandler != null)
                            resendStatusHandler.removeCallbacksAndMessages(null);
                        resendStatusAttempts = 0;
                    } else {
                        Toast.makeText(MainActivity.this, "The request status has been updated successfully", Toast.LENGTH_SHORT).show();
                        current_request.nextStatus();
                        if (status.equals("completed")) {
                            endRequest(REQUEST_SUCCESS);
                        }
                    }
                    setUI();

                } else if (response.code() == 401) {
                    Toast.makeText(MainActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "sendStatus: User not logged in");
                    logout();
                } else {
                    Toast.makeText(MainActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Unknown error occurred");
/*
                    if (status.equals("on_the_way")) {
                        if (resendStatusHandler == null)
                            resendStatusHandler = new Handler();
                        if (resendStatusAttempts * resendFailedRequestDelay < RESENDING_ATTEMPTS_OVERALL_DELAY) {
                            resendStatusAttempts++;
                            resendStatusHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendStatus(request_id, status);
                                }
                            }, resendFailedRequestDelay);
                        } else {
                            Log.d(TAG, String.format("Couldn't connect to the server after %d minutes... Stopping now.", RESENDING_ATTEMPTS_OVERALL_DELAY / 60 / 1000));
                        }
                    }
*/
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if (!status.equals("on_the_way"))
                    if (!MainActivity.this.isFinishing() && progress != null && progress.isShowing())
                        progress.dismiss();
                Toast.makeText(MainActivity.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.d(TAG, getString(R.string.server_timeout));
/*
                if (status.equals("on_the_way")) {
                    if (resendStatusHandler == null)
                        resendStatusHandler = new Handler();
                    if (resendStatusAttempts * resendFailedRequestDelay < RESENDING_ATTEMPTS_OVERALL_DELAY) {
                        resendStatusAttempts++;
                        resendStatusHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendStatus(request_id, status);
                            }
                        }, resendFailedRequestDelay);
                    } else {
                        Log.d(TAG, String.format("Couldn't connect to the server after %d minutes... Stopping now.", RESENDING_ATTEMPTS_OVERALL_DELAY / 60 / 1000));
                    }

                }
*/
            }
        });
    }

    private void sendActive(final int active, final String location) {
        Log.d(TAG, "sendActive");
        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(MainActivity.this))
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
//                    Toast.makeText(MainActivity.this, R.string.driver_status_changed_successfully, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The driver status has been set successfully");
                    if (prefManager.isActive() || prefManager.getGoActive()) {
                        if(prefManager.isActive()){
                            if(mIsBound){
                                backgroundLocationService.sendActive(1, prefManager.getCurrentLocation());
                            }
                        }
                    } else {
                        prefManager.setActive(false);
                        resendActiveAttempts = 0;
                        if(resendActiveHandler != null)
                            resendActiveHandler.removeCallbacksAndMessages(null);
                    }
                    setUI();
                } else if (response.code() == 401) {
                    Log.i(TAG, "onResponse: User not logged in");
//                    Toast.makeText(MainActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    logout();
                } else {
                    Log.i(TAG, "Unknown error occurred");
//                    Toast.makeText(MainActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    if (prefManager.isActive() || prefManager.getGoActive()){
                        if(resendActiveHandler != null) resendActiveHandler.removeCallbacksAndMessages(null);
                        resendActiveAttempts = 0;
                    }
                    else{
                        if(active == 0) {
                            if (resendActiveHandler == null)
                                resendActiveHandler = new Handler();
                            if (resendActiveAttempts * resendFailedRequestDelay < RESENDING_ATTEMPTS_OVERALL_DELAY) {
                                resendActiveAttempts++;
                                resendActiveHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendActive(active, location);
                                    }
                                }, resendFailedRequestDelay);
                            } else {
                                Log.d(TAG, String.format("Couldn't connect to the server after %d minutes... Stopping now.", RESENDING_ATTEMPTS_OVERALL_DELAY / 60 / 1000));
                            }
                        }
                    }
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
//                Toast.makeText(MainActivity.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.i(TAG, getString(R.string.server_timeout));
                if (prefManager.isActive() || prefManager.getGoActive()) {
                    if (resendActiveHandler != null)
                        resendActiveHandler.removeCallbacksAndMessages(null);
                    resendActiveAttempts = 0;
                } else {
                    if (active == 0) {
                        if (resendActiveHandler == null)
                            resendActiveHandler = new Handler();
                        if (resendActiveAttempts * resendFailedRequestDelay < RESENDING_ATTEMPTS_OVERALL_DELAY) {
                            resendActiveAttempts++;
                            resendActiveHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendActive(active, location);
                                }
                            }, resendFailedRequestDelay);
                        } else {
                            Log.d(TAG, String.format("Couldn't connect to the server after %d minutes... Stopping now.", RESENDING_ATTEMPTS_OVERALL_DELAY / 60 / 1000));
                        }
                    }
                }
            }
        });
    }

    private void sendCancel(final String request_id) {
        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(MainActivity.this))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        progress = new ProgressDialog(this);
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
                if (!MainActivity.this.isFinishing() && progress != null && progress.isShowing())
                    progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
                    endRequest(REQUEST_CANCELLED);
                    setUI(UI_STATE.SIMPLE);
                    Toast.makeText(MainActivity.this, R.string.request_cancelled_successfully, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The request has been cancelled successfully");
                } else if (response.code() == 401) {
                    Toast.makeText(MainActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "sendCancel: User not logged in");
                    logout();
                } else {
                    Toast.makeText(MainActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if (!MainActivity.this.isFinishing() && progress != null && progress.isShowing())
                    progress.dismiss();
                Toast.makeText(MainActivity.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.i(TAG, getString(R.string.server_timeout));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,String.format("onActivityResult: requestCode = %d, resultCode = %d", requestCode, resultCode));
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                case RESULT_OK:
                    Log.i(TAG, "User agreed to make required location settings changes.");
                    if(mIsBound)
                        backgroundLocationService.startLocationUpdates();
                    break;
                case RESULT_CANCELED:
                    Log.i(TAG, "User chose not to make required location settings changes.");
                    prefManager.setActive(false);
                    prefManager.setGoActive(false);
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    if (mIsBound) {
//                        backgroundLocationService.setGoActive(false);
                        getApplicationContext().unbindService(mConnection);
                        mIsBound = false;
                    }
                    if (blsIntent != null)
                        stopService(blsIntent);
                    setUI();
                    break;
            }
        }
    }

    private void getCurrentRequest(Intent data) {
        if (data.hasExtra("request_id")) {
            //set the data
            request temp = new request();
            temp.setPassenger_name(data.getExtras().getString("passenger_name"));
            temp.setPassenger_phone(data.getExtras().getString("passenger_phone"));
            temp.setStatus(data.getExtras().getString("status"));
            temp.setPickupString(data.getStringExtra("pickup"));
            temp.setDestString(data.getStringExtra("dest"));
            temp.setTime(data.getExtras().getString("time"));
            temp.setNotes(data.getExtras().getString("notes"));
            temp.setPrice(data.getExtras().getString("price"));
            temp.setDestText(data.getStringExtra("dest_text"));
            temp.setPickupText(data.getStringExtra("pickup_text"));
            temp.setRequest_id(data.getStringExtra("request_id"));
            createdFromNewRequest = true;
            prefManager.setRequest(temp);
            startRequest();
        }
    }

    private void startRequest() {
        current_request = prefManager.getRequest();
        prefManager.setDoingRequest(true);
        EventBus.getDefault().post(new ChangeActiveUpdateInterval());
//        sendStatus(current_request.getRequest_id(), current_request.getStatus());
    }

    public enum UI_STATE {
        SIMPLE,
        DOINGREQUEST
    }

    void setUI() {
        if (prefManager.isDoingRequest())
            setUI(UI_STATE.DOINGREQUEST);
        else
            setUI(UI_STATE.SIMPLE);
    }

    public void setUI(UI_STATE state) {
        switch (state) {
            case SIMPLE:
                if (routingHandler != null) {
                    Log.d(TAG,"setUI Simple: removing routing runnable");
                    routeCancelled = true;
                    routingHandler.removeCallbacksAndMessages(null);
                }
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
                linearLayout.setVisibility(View.INVISIBLE);
                findViewById(R.id.change_driver_status).setVisibility(View.VISIBLE);
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

                findViewById(R.id.nav_button).setVisibility(View.GONE);

                if (prefManager.isActive()) {

                    ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_inactive);
                    ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.colorAccent));
                    findViewById(R.id.change_driver_status).setBackgroundColor(getResources().getColor(R.color.white));
                    ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_active));
                    ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));

                } else {
//                    if(mIsBound){
//                        if(backgroundLocationService.getGoActive()) {
                        if(prefManager.getGoActive()) {
                        ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.colorAccent));
                        findViewById(R.id.change_driver_status).setBackgroundColor(getResources().getColor(R.color.white));
                        ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_inactive);
                        ((TextView) findViewById(R.id.toolbar_title)).setText("Going Active..");
                        ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
                    }
/*
                        else {
                        ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.white));
                        findViewById(R.id.change_driver_status).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorAccent));
                        ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_inactive));
                        ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_active);
                    }
*/
//                    }
                    else {
                        ((TextView) findViewById(R.id.change_driver_status)).setTextColor(getResources().getColor(R.color.white));
                        findViewById(R.id.change_driver_status).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorAccent));
                        ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_inactive));
                        ((TextView) findViewById(R.id.change_driver_status)).setText(R.string.go_active);
                    }
                }
                break;

            case DOINGREQUEST:
                pickupPoint = new LatLng(current_request.getPickup()[0], current_request.getPickup()[1]);
                destPoint = new LatLng(current_request.getDest()[0], current_request.getDest()[1]);


                //set values for the different views
                TextView nextState;
                TextView current;
                linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
                nextState = (TextView) findViewById(R.id.next_state);
                ((TextView) findViewById(R.id.toolbar_title)).setText((current_request.getDisplayStatus(current_request.getStatus(), MainActivity.this)));
                ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
                nextState.setText(current_request.getDisplayStatus(current_request.getNextStatus(), MainActivity.this));
                linearLayout.setVisibility(View.VISIBLE);

                findViewById(R.id.nav_button).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.cr_passenger_name)).setText(current_request.getPassenger_name());

                findViewById(R.id.change_driver_status).setVisibility(View.INVISIBLE);
                if (current_request.getStatus().equals("passenger_onboard") ||
                        current_request.getStatus().equals("arrived_dest") ||
                        current_request.getStatus().equals("completed")) {
                    findViewById(R.id.request_view_top).setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.cancel_request)).setText(R.string.ride_info);
                    ((TextView) findViewById(R.id.cancel_request)).setTextColor(getResources().getColor(R.color.colorPrimary));
                } else {
                    findViewById(R.id.request_view_top).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.cancel_request)).setText(getString(R.string.cancel));
                    ((TextView) findViewById(R.id.cancel_request)).setTextColor(getResources().getColor(R.color.red2));
                }

                setOnClickListeners();
                if (mapIsReady)
                    setMarkers();
                else
                    setWhenReady = true;

        }
    }

    private void setMarkers() {
        Log.d(TAG, "setMarkers has been called");
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

        if(routingHandler == null)
            routingHandler = new Handler();
        routingRunnable = new Runnable() {
            public void run() {
                //
                // Do the stuff
                //

                routeCancelled = false;
                try {
                    showRoute();
                } finally {
                    // 100% guarantee that this always happens, even if
                    // your update method throws an exception
                    routingHandler.postDelayed(this, 30 * 1000);
                }
            }
        };
        routingRunnable.run();
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
        if (prefManager.isDoingRequest()) {
            prefManager.setRequest(current_request);
        }
        checkedLocation = false;
        if (routingHandler != null) {
            Log.d(TAG,"onPause: removing routing runnable");
            routeCancelled = true;
            routingHandler.removeCallbacksAndMessages(null);
        }
        wasActive = prefManager.isActive();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (!prefManager.isLoggedIn()) {
            logout();
        }
        else {
            //get the driver information
            driver = prefManager.getDriver();
            if (prefManager.isActive() || prefManager.getGoActive()) {
                if (backgroundLocationService != null)
                    if (!checkedLocation) {
                        backgroundLocationService.checkLocationSettings();
                        checkedLocation = true;
                    }
            } else if (prefManager.isDoingRequest()) {
                startAndBindLocationService();
            }


            //Setup the UI based on whether there is a current request
            if (prefManager.isDoingRequest()) {
                current_request = prefManager.getRequest();
                if (prefManager.getFcmrequestId().equals(current_request.getRequest_id())) {
                    if (prefManager.getFcmrequestStatus().equals("completed")) {
                        Log.d(TAG, "The passenger marked the request as complete");
                        endRequest(REQUEST_SUCCESS);
                        setUI(UI_STATE.SIMPLE);
                        current_request = new request();
                        prefManager.setRequest(current_request);
                    } else if (prefManager.getFcmrequestStatus().equals("canceled")) {
                        Log.d(TAG, "The passenger canceled the request");
                        endRequest(REQUEST_CANCELLED);
                        setUI(UI_STATE.SIMPLE);
                        current_request = new request();
                        prefManager.setRequest(current_request);
                    }
                } else {
                    Log.d(TAG, "The driver is doing a request");
                    setUI(UI_STATE.DOINGREQUEST);
                }
            } else {
                setUI(UI_STATE.SIMPLE);
            }

            //remove the fcm request update

            if (!prefManager.getFcmrequestId().equals("No data")) {
                prefManager.setFcmrequestId("No data");
                prefManager.setFcmrequestStatus("No data");
            }
        }

    }



    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassengerCancelled(PassengerCanceled event) {
        Log.d(TAG, "onPassengerCanceled has been invoked");
        if(prefManager.isDoingRequest()) {
            if(event.getRequest_id().equals(current_request.getRequest_id())) {
                endRequest(REQUEST_CANCELLED);
                setUI();
                current_request = new request();
                prefManager.setRequest(current_request);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverLoggedout(DriverLoggedout event) {
        Log.d(TAG, "onDriverLoggedout has been invoked");
        logout();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassngerArrived(PassengerArrived event) {
        Log.d(TAG, "onPassengerArrived has been invoked");
        if(prefManager.isDoingRequest()) {
            if(event.getRequest_id().equals(current_request.getRequest_id())) {
                endRequest(REQUEST_SUCCESS);
                setUI();
                current_request = new request();
                prefManager.setRequest(current_request);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverActive(DriverActive event) {
        Log.d(TAG, "onDriverActive has been invoked");
//        if(mIsBound){
//            backgroundLocationService.setGoActive(false);
//        }
//        if (goActive) { //if the user changed his status to active
//            goActive = false;
//        }
        prefManager.setGoActive(false);
        setUI();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnbindBackgroundLocationService(UnbindBackgroundLocationService event) {
        Log.d(TAG, "onUnbindBackgroundLocationService has been invoked");
        prefManager.setGoActive(false);
        if (mIsBound) {
//            backgroundLocationService.setGoActive(false);
            getApplicationContext().unbindService(mConnection);
//            goActive = false;
            mIsBound = false;
            setUI();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLocationUpdated(LocationUpdated event) {
        Log.d(TAG, "onLocationUpdated (Eventbus) has been invoked");

        mCurrentLocation = event.getLocation();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.d(TAG, "onLocationChanged: mLocation: " + mCurrentLocation.toString());
        if (firstMove && mLastLocation != null) {
            Log.d(TAG, "onLocationChanged: Moving cam");
            Log.d(TAG, "onLocationChanged: mLocation: " + mLastLocation.toString());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 12.0f));
            firstMove = false;
        }
        currentLocationPoint = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        if (prefManager.isDoingRequest()) if (firstLocationToDriverRouting) {
            routeCancelled = false;
            showRoute();
            firstLocationToDriverRouting = false;
        }

        if (!mMap.isMyLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.d(TAG, "onLocationUpdated (Eventbus) mMap.setMyLocationEnabled(true)");
            mMap.setMyLocationEnabled(true);
            // Get the button view
            View locationButton = ((View) findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

            // and next place it, for exemple, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            // position on right bottom
            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
            int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            rlp.setMargins(0, 0, right, bottom);
        }

    }

    @Override
    public void onStop() {
        Log.d(TAG,"onStop");
        if(mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


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
            logout();
        } else if (id == R.id.language) {
            Configuration config = new Configuration();
            if (item.getTitle().equals("English")) {
                String languageToLoad = "en";
                Locale locale = new Locale(languageToLoad);
                Locale.setDefault(locale);
                config.locale = locale;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    config.setLayoutDirection(locale);
                }
                if (item.getTitle().equals(R.string.Language))
                    prefManager.setOtherLanguage(true);
                else
                    prefManager.setOtherLanguage(false);
            } else {
                String languageToLoad = "ar";
                Locale locale = new Locale(languageToLoad);
                Locale.setDefault(locale);
                config.locale = locale;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    config.setLayoutDirection(locale);
                }
                if (item.getTitle().equals(R.string.Language))
                    prefManager.setOtherLanguage(true);
                else
                    prefManager.setOtherLanguage(false);
            }

            Context context = getApplicationContext();
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        String lastEmail = prefManager.getLastEmail();
        String lastPassword = prefManager.getLastPassword();
        prefManager.editor.clear().apply();
        prefManager.setLastPassword(lastPassword);
        prefManager.setLastEmail(lastEmail);
        prefManager.setIsLoggedIn(false);
        driver = new driver();
        if (mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
        EventBus.getDefault().post(new UnbindBackgroundLocationService());
        if (blsIntent != null)
            stopService(blsIntent);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        mapIsReady = true;
        LatLng khartoum = new LatLng(15.5838046, 32.5543825);
        if (firstMove)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(khartoum, 12));

        if (setWhenReady) {
            setWhenReady = false;
            if (!createdFromNewRequest) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        // Actions to do after 10 seconds
                        setMarkers();
                    }
                }, 2000);
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d(TAG, "onMapReady: mMap.setMyLocationEnabled(true)");
        mMap.setMyLocationEnabled(true);
//        mMap.setPadding(0, 150, 0, 0);
        // Get the button view
        View locationButton = ((View) findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

        // and next place it, for exemple, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        rlp.setMargins(0, 0, right, bottom);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_CODE) {
            Log.d(TAG, "ACCESS_FINE_LOCATION_CODE onRequestPermissionsResult:");
            permissionIsRequested = false;
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "ACCESS_FINE_LOCATION_CODE onRequestPermissionsResult: Granted");
                if (!checkedLocation) {
                    backgroundLocationService.checkLocationSettings();
                    checkedLocation = true;
                }
            } else {
                Toast.makeText(this, R.string.location_permission_denied,
                        Toast.LENGTH_LONG).show();
                prefManager.setActive(false);
                setUI();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }

    private void showRoute() {
        Log.d(TAG, "showRoute: Called");
        if (currentLocationPoint == null)
            currentLocationPoint = new LatLng(Double.parseDouble(prefManager.getCurrentLocation().split(",")[0]),
                    (Double.parseDouble(prefManager.getCurrentLocation().split(",")[1])));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocationPoint.latitude, currentLocationPoint.longitude), 15.0f));
        // Pan to see all markers in view.
        if (current_request.getStatus().equals("passenger_onboard") ||
                current_request.getStatus().equals("arrived_dest") ||
                current_request.getStatus().equals("completed")) {
            if (currentLocationPoint != null) {
                GoogleDirection.withServerKey(GOOGLE_DIRECTIONS_API)
                        .from(currentLocationPoint)
                        .to(destPoint)
                        .execute(new DirectionCallback() {
                            @Override
                            public void onDirectionSuccess(Direction direction, String rawBody) {
                                // Do something here
                                Log.d(TAG, "showRoute:Driver to Pickup Route successfully computed ");
                                if (routeCancelled)
                                    return;

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
                                    PolylineOptions polylineOptions = DirectionConverter.createPolyline(MainActivity.this, directionPositionList, 5, getResources().getColor(R.color.red2));
                                    if (pickupToDestRoute != null) {
                                        pickupToDestRoute.remove();
                                    }
                                    if (driverToPickupRoute != null) {
                                        driverToPickupRoute.remove();
                                    }

                                    if (pickupMarker != null)
                                        pickupMarker.remove();
                                    driverToPickupRoute = mMap.addPolyline(polylineOptions);

                                } else {
                                    //TODO
                                }

                            }

                            @Override
                            public void onDirectionFailure(Throwable t) {
                                // Do something here
                                Log.d(TAG, "showRoute:Driver to Pickup Route Failed ");
                                if (routeCancelled)
                                    return;
                                Toast.makeText(MainActivity.this, R.string.driver_to_pickup_route_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

        } else {
            GoogleDirection.withServerKey(GOOGLE_DIRECTIONS_API)
                    .from(pickupPoint)
                    .to(destPoint)
                    .execute(new DirectionCallback() {
                        @Override
                        public void onDirectionSuccess(Direction direction, String rawBody) {
                            // Do something here
                            Log.d(TAG, "showRoute:Pickup to Destination Route successfully computed ");

                            if (routeCancelled)
                                return;

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
                                PolylineOptions polylineOptions = DirectionConverter.createPolyline(MainActivity.this, directionPositionList, 5, getResources().getColor(R.color.red2));
                                if (pickupToDestRoute != null) {
                                    pickupToDestRoute.remove();
                                }


                                pickupToDestRoute = mMap.addPolyline(polylineOptions);

                            }

                        }

                        @Override
                        public void onDirectionFailure(Throwable t) {
                            // Do something here
                            Log.d(TAG, "showRoute:Pickup to Destination Route Failed ");
                            if (routeCancelled)
                                return;
                            Toast.makeText(MainActivity.this, R.string.pickup_to_dest_route_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
            if (currentLocationPoint != null) {
                GoogleDirection.withServerKey(GOOGLE_DIRECTIONS_API)
                        .from(currentLocationPoint)
                        .to(pickupPoint)
                        .execute(new DirectionCallback() {
                            @Override
                            public void onDirectionSuccess(Direction direction, String rawBody) {
                                // Do something here
                                Log.d(TAG, "showRoute:Driver to Pickup Route successfully computed ");
                                if (routeCancelled)
                                    return;

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
                                Log.d(TAG, "showRoute:Driver to Pickup Route Failed ");
                                if (routeCancelled)
                                    return;
                                Toast.makeText(MainActivity.this, R.string.driver_to_pickup_route_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (!MainActivity.this.isFinishing() && progress != null && progress.isShowing())
            progress.dismiss();
        if (mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
    }


}
