package com.vogella.android.navigationwidgetattempt;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
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
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final long UPDATE_DURING_REQUEST = 10 * 1000; //10 seconds
    private static final long UPDATE_WHILE_IDLE = 30 * 1000;
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
    private LatLng currentLocationPoint;
    private Marker currentLocationMarker;
    private Polyline routePolyline;

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
    private static final String DUMMY_TIME = "06/11/2016 ; 15:45";
    private static request current_request = new request(DUMMY_REQUEST_ID, DUMMY_PICKUP, DUMMY_DEST,
            DUMMY_PASSENGER_NAME, DUMMY_PASSENGER_PHONE, DUMMY_TIME, DUMMY_PRICE, DUMMY_NOTES,
            DUMMY_STATUS);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final Button changeDriverStatus = (Button) findViewById(R.id.driver_status);
        changeDriverStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (changeDriverStatus.getText().toString().equals("available")) {
                    changeDriverStatus.setText("away");
                    sendActive(false, "15.6023428, 32.5873593");
                } else if (changeDriverStatus.getText().toString().equals("away")){
                    changeDriverStatus.setText("available");
                    sendActive(true, "15.6023428, 32.5873593");
                }
            }
        });

        Button cancelRequest = (Button) findViewById(R.id.cancel_request);
        cancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alerBuilder = new AlertDialog.Builder(MainActivity.this);
                alerBuilder.setMessage("Are you sure you want to cancel this request?");
                alerBuilder.setPositiveButton("Yes, cancel the request", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendCancel(current_request.getRequest_id());
                    }
                });
                alerBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alerBuilder.show();
            }
        });
        final Button nextState = (Button) findViewById(R.id.next_state);
        nextState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView current = (TextView) findViewById(R.id.current_status);
                current.setText(nextState.getText().toString());
                current_request.nextStatus();
                sendStatus(current_request.getRequest_id(), current_request.getStatus());
                if (current_request.getStatus().equals("completed")) {
                    endRequest(REQUEST_SUCCESS);
                } else
                    nextState.setText(current_request.getDisplayStatus(current_request.getNextStatus()));
            }
        });

        TextView current = (TextView) findViewById(R.id.current_status);
        current.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.current_request_view).setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.cr_time)).setText(current_request.getTime());
                ((TextView)findViewById(R.id.cr_passenger_name)).setText(current_request.getPassenger_name());
                ((TextView)findViewById(R.id.cr_passenger_phone)).setText(current_request.getPassenger_phone());
                ((TextView)findViewById(R.id.cr_price)).setText(current_request.getPrice());
                ((TextView)findViewById(R.id.cr_status)).setText(current_request.getDisplayStatus(current_request.getStatus()));
                ((TextView)findViewById(R.id.cr_notes)).setText(current_request.getNotes());
                ((TextView)findViewById(R.id.cr_pickup)).
//                        setText(current_request.pickup);
                        setText(String.valueOf(current_request.getPickup()[0]) + " , " +
                                String.valueOf(current_request.getPickup()[1]));
                ((TextView)findViewById(R.id.cr_dest)).
                        setText(String.valueOf(current_request.getDest()[0]) + " , " +
                                String.valueOf(current_request.getDest()[1]));
                ((TextView)findViewById(R.id.cr_time)).setText(current_request.getTime());

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
                        alerBuilder.setMessage("Do you want to call the number " + current_request.getPassenger_phone() + "?");
                        alerBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:".concat(current_request.getPassenger_phone())));
                                startActivity(intent);
                            }
                        });
                        alerBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        alerBuilder.show();                    }
                });

            }
        });
//        previous_requests = (RecyclerView) findViewById(R.id.past_requests);
//        previous_requests.setHasFixedSize(true);
//        layoutManager = new LinearLayoutManager(this);
//        previous_requests.setLayoutManager(layoutManager);

        prefManager = new PrefManager(this);
        driver = prefManager.getDriver();
        ((TextView) (navigationView.inflateHeaderView(R.layout.nav_header_main))
                            .findViewById(R.id.show_username)).setText(driver.getUsername());

        prefManager.setCurrentLocation(DUMMY_DRIVER_LOCATION);

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, UpdateLocation.class);
        boolean flag = (PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_NO_CREATE)==null);
/*Register alarm if not registered already*/
//        if(flag){
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

/* Setting alarm for every one hour from the current time.*/
            int intervalTimeMillis;
            if(prefManager.isDoingRequest())
                intervalTimeMillis = 20 * 1000;
            else
                intervalTimeMillis = 30 * 1000;//30 * 60 * 1000; // 30 minutes
            alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(), intervalTimeMillis,
                    alarmIntent);
//        }

    }

    void endRequest(int res){
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
        linearLayout.setVisibility(View.INVISIBLE);
        if (res == REQUEST_SUCCESS)
            Toast.makeText(MainActivity.this, "Thank you for your efforts! The request is complete",
                    Toast.LENGTH_LONG).show();
        else if(res == REQUEST_CANCELLED)
            Toast.makeText(MainActivity.this, "The request has been canceled",
                    Toast.LENGTH_LONG).show();
        OngoingRequestsActivity.removeRequest(current_request.getRequest_id());
        current_request = new request();
        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (destMarker != null) {
            destMarker.remove();
        }
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        if (routePolyline != null) {
            routePolyline.remove();
        }
        prefManager.setRequestId("");
        prefManager.setDoingRequest(false);

        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), UPDATE_WHILE_IDLE,
                alarmIntent);
    }

    private void sendStatus(String request_id, final String status ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("USER_EMAIL","");
        String password = prefManager.pref.getString("USER_PASSWORD","");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.status("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                request_id,status);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    Log.d(TAG, "The status has been sent successfully");
                } else if (response.code() == 401){
                    Toast.makeText(MainActivity.this, "Please login to continue", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(intent);
                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {

            }
        });
    }
    private void sendActive (boolean active, final String location ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("USER_EMAIL","");
        String password = prefManager.pref.getString("USER_PASSWORD","");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.active("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                active, location);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    Log.d(TAG, "The status has been changed successfully");
                } else if (response.code() == 401){
                    Toast.makeText(MainActivity.this, "Please login to continue", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(intent);
                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {

            }
        });
    }
    private void sendCancel (final String request_id ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("USER_EMAIL","");
        String password = prefManager.pref.getString("USER_PASSWORD","");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.cancel("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                request_id);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    endRequest(REQUEST_CANCELLED);
                    Log.d(TAG, "The request has been cancelled successfully");
                } else if (response.code() == 401){
                    Toast.makeText(MainActivity.this, "Please login to continue", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(intent);
                    MainActivity.super.finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {

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
                current_request = new request();
                current_request.setPassenger_name(data.getExtras().getString("passenger_name"));
                current_request.setPassenger_phone(data.getExtras().getString("passenger_phone"));
                current_request.setStatus(data.getExtras().getString("status"));
                double pickup[] = new double[2];
                pickup[0] = data.getExtras().getDouble("pickup_longitude");
                pickup[1] = data.getExtras().getDouble("pickup_latitude");
                current_request.setPickup(pickup);
                double dest[] = new double[2];
                dest[0] = data.getExtras().getDouble("dest_longitude");
                dest[1] = data.getExtras().getDouble("dest_latitude");
                current_request.setDest(dest);
                current_request.setTime(data.getExtras().getString("time"));
                current_request.setNotes(data.getExtras().getString("notes"));
                current_request.setPrice(data.getExtras().getString("price"));
                current_request.setRequest_id(data.getExtras().getString("request_id"));
                startRequest();


            }
        }
    }

    private void startRequest() {
        //                pickupPoint = new LatLng(pickup[0], pickup[1]);

        prefManager.setDoingRequest(true);
        prefManager.setRequestId(current_request.getRequest_id());
        pickupPoint = new LatLng(current_request.getPickup()[0], current_request.getPickup()[1]);
        destPoint = new LatLng(current_request.getDest()[0], current_request.getDest()[1]);

        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), UPDATE_DURING_REQUEST,
                alarmIntent);

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

        //set values for the different views
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ongoing_request);
        Button nextState = (Button) findViewById(R.id.next_state);
        TextView current = (TextView) findViewById(R.id.current_status);
        current.setText(current_request.getDisplayStatus(current_request.getStatus()));
        String temp = current_request.getStatus();
        current_request.nextStatus();
        nextState.setText(current_request.getDisplayStatus(current_request.getStatus()));
        current_request.setStatus(temp);
        linearLayout.setVisibility(View.VISIBLE);
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
//        if(prefManager.isDoingRequest()){
//            Gson gson = new Gson();
//            String json = gson.toJson(current_request);
//            prefManager.editor.putString("current_request", json);
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        if(prefManager.isDoingRequest()){
//            Gson gson = new Gson();
//            String json = prefManager.pref.getString("current_request","");
//            current_request = gson.fromJson(json, request.class);
//            startRequest();
//        }
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassengerCancelled(PassengerCanceled event) {
        endRequest(REQUEST_CANCELLED);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverLoggedout(DriverLoggedout event) {
        prefManager.setIsLoggedIn(false);
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        MainActivity.this.startActivity(intent);
        MainActivity.super.finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassngerArrived(PassengerArrived event) {
        endRequest(REQUEST_SUCCESS);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = new Intent(this, FCMRequest.class);
        intent.putExtra("request_id","13213");
        intent.putExtra("price","33");
        intent.putExtra("pickup","15.6023428, 32.5873593");
        intent.putExtra("dest","15.5023428, 32.3873593");
//        intent.putExtra("time","25/10/16 4:33");
        //intent.putExtra("time","1479935269");
        intent.putExtra("time","now");
        intent.putExtra("passenger_name","George Washington");
        intent.putExtra("passenger_phone","0999999999");
        intent.putExtra("notes","Don't smoke! Don't drive while texting!");
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        String token = FirebaseInstanceId.getInstance().getToken();

        // Log and toast
        String msg = getString(R.string.msg_token_fmt, token);
        Log.d(TAG, msg);

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);

        } else if (id == R.id.current_requests) {
            Intent intent = new Intent(this, OngoingRequestsActivity.class);
            startActivityForResult(intent, ONGOING_REQUESTS_CODE);

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

        // Add a marker in Sydney and move the camera
        LatLng khartoum = new LatLng(15.5838046, 32.5543825);
//        mMap.addMarker(new MarkerOptions().position(khartoum).title("Marker in Khartoum"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(khartoum, 14 ));

        // ==================== To get location ================

        // Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        // Location Request
        mLocationRequest = new LocationRequest();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(5000);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(1000);


        // Check device location settings
        LocationSettingsRequest.Builder locationSettingsReqBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        locationSettingsReqBuilder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
//                final LocationSettingsStates = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    0x1);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.

                        break;
                }
            }
        });

    }




    @Override
    public void onConnected(Bundle bundle) {


        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, (LocationListener) this);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mCurrentLocation = mLastLocation;
            Toast.makeText(this, "Connected GPlServices "+mLastLocation.getLatitude()+" "+mLastLocation.getLongitude(), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Sorry, it's null", Toast.LENGTH_SHORT).show();

        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
    @Override
    public void onLocationChanged(Location location) {

        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        currentLocationPoint = new LatLng(location.getLatitude(), location.getLongitude());

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        currentLocationMarker= mMap.addMarker(new MarkerOptions()
                .position(currentLocationPoint)
                .title("Current Location")
        );

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
                        Log.d(TAG, "showRoute: Route successfully computed ");

                        if(direction.isOK()) {
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
                            if (routePolyline != null) {
                                routePolyline.remove();
                            }
                            routePolyline = mMap.addPolyline(polylineOptions);

                        }

                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something here
                        Toast.makeText(MainActivity.this, "Route Failed ", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "showRoute: Route Failed ");
                    }
                });
    }

}
