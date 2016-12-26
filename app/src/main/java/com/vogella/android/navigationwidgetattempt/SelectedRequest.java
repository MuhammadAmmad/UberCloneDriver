package com.vogella.android.navigationwidgetattempt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.PassengerCanceled;
import com.Wisam.Events.UnbindBackgroundLocationService;
import com.Wisam.POJO.StatusResponse;
import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.google.android.gms.maps.model.LatLng;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SelectedRequest extends AppCompatActivity {

    private static final String TAG = "Selected Request";
    private Intent intent;
    private PrefManager prefManager;
    private ProgressDialog progress;
    private static final String GOOGLE_DIRECTIONS_API = "AIzaSyDpJmpRN0BxJ76X27K0NLTGs-gDHQtoxXQ";
    private LatLng pickupPoint;
    private LatLng destPoint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        intent = getIntent();

        if(intent.hasExtra("source")) {
            if (intent.getStringExtra("source").equals("history")) {
                if (intent.getStringExtra("status").equals("completed"))
                    setTheme(R.style.AppTheme_details_completed);
                else if (intent.getStringExtra("status").equals("missed"))
                    setTheme(R.style.AppTheme_details_missed);
                else if (intent.getStringExtra("status").equals("canceled"))
                    setTheme(R.style.AppTheme_details_canceled);
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_request);
        Toolbar toolbar = (Toolbar) findViewById(R.id.request_details_toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.white));
        ((TextView) findViewById(R.id.request_details_toolbar_title)).setTextColor(getResources().getColor(R.color.white));
//        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
//        toolbar.setTitle(R.string.driver_active);
//        toolbar.setContentInsetsAbsolute(0,0);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefManager = new PrefManager(this);


        if(intent.hasExtra("source")) {
            if (intent.getStringExtra("source").equals("history")) {
                ((LinearLayout) findViewById(R.id.request_details_buttons)).setVisibility(View.GONE);
                ImageView icon = (ImageView) findViewById(R.id.derails_icon);
                ((TextView) findViewById(R.id.request_details_toolbar_title)).setTextColor(getResources().getColor(R.color.white));
                if (intent.getStringExtra("status").equals("completed")) {
//                    getTheme().applyStyle(R.style.AppTheme_details_completed,true);
                    toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_request_completed));
                    ((TextView) findViewById(R.id.request_details_toolbar_title)).setText("Completed");

//                    icon.setBackground(getResources().getDrawable(R.drawable.ic_request_completed));
                } else if (intent.getStringExtra("status").equals("canceled")) {
//                    getTheme().applyStyle(R.style.AppTheme_details_canceled,true);
                    toolbar.setBackgroundColor(getResources().getColor(R.color.red2));
                    icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_request_canceled));
                    ((TextView) findViewById(R.id.request_details_toolbar_title)).setText("Cancelled");
                } else if (intent.getStringExtra("status").equals("missed")) {
                    getTheme().applyStyle(R.style.AppTheme_details_missed,true);
                    toolbar.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                    icon.setImageDrawable(getResources().getDrawable(R.drawable.request_missed));
                    ((TextView) findViewById(R.id.request_details_toolbar_title)).setText("Missed");
                }
            }
            else if (intent.getStringExtra("source").equals("incoming") || intent.getStringExtra("source").equals("FCMRequest")){
                pickupPoint = new LatLng(Double.valueOf(intent.getStringExtra("pickup").split(",")[0]),
                        Double.valueOf(intent.getStringExtra("pickup").split(",")[1]));
                destPoint = new LatLng(Double.valueOf(intent.getStringExtra("dest").split(",")[0]),
                        Double.valueOf(intent.getStringExtra("dest").split(",")[1]));
                calculate_distance();
                ImageView icon = (ImageView) findViewById(R.id.derails_icon);
                icon.setVisibility(View.GONE);
                ((TextView) findViewById(R.id.request_details_toolbar_title)).setPadding(0, 15, 0, 15);
                ((TextView) findViewById(R.id.request_details_toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        }
        else {
            Log.d(TAG,"This shouldn't be happening.. All intents to this activity are presumed to have a \'source\' extra");
            ImageView icon = (ImageView) findViewById(R.id.derails_icon);
            icon.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.request_details_toolbar_title)).setPadding(0, 15, 0, 15);
            ((TextView) findViewById(R.id.request_details_toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
        }

        ((TextView) findViewById(R.id.request_details_pickup)).setText(intent.getStringExtra("pickup_text"));
        ((TextView) findViewById(R.id.request_details_dest)).setText(intent.getStringExtra("dest_text"));
        ((TextView) findViewById(R.id.request_details_price)).setText(intent.getStringExtra("price"));
        ((TextView) findViewById(R.id.request_details_time)).setText(intent.getStringExtra("time"));
        ((TextView) findViewById(R.id.request_details_notes)).setText(intent.getStringExtra("notes"));

        ((TextView) findViewById(R.id.request_details_start)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                setResult(RESULT_OK, intent);
                Intent intent2 = new Intent(SelectedRequest.this, MainActivity.class);
                intent2.putExtras(intent);
                startActivity(intent2);
                finish();
            }
        });
        ((TextView) findViewById(R.id.request_details_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new PassengerCanceled(intent.getStringExtra("request_id")));
                sendCancel(intent.getStringExtra("request_id"));

            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id==android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    private void sendCancel(final String request_id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
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
                if (!SelectedRequest.this.isFinishing() && progress != null && progress.isShowing()) progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
//                    endRequest(REQUEST_CANCELLED);
//                    setUI(MainActivity.UI_STATE.SIMPLE);
                    Toast.makeText(SelectedRequest.this, R.string.request_cancelled_successfully, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The request has been cancelled successfully");
                    Intent temp = new Intent();
                    setResult(RESULT_OK, temp);
                    finish();
                } else if (response.code() == 401) {
                    Toast.makeText(SelectedRequest.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onResponse: User not logged in");
                    String lastEmail = prefManager.getLastEmail();
                    String lastPassword = prefManager.getLastPassword();
                    prefManager.editor.clear().apply();
                    prefManager.setLastPassword(lastPassword);
                    prefManager.setLastEmail(lastEmail);
                    prefManager.setIsLoggedIn(false);
//                    prefManager.setExternalLogout(false);
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    Intent blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);
                    stopService(blsIntent);

                    Intent intent = new Intent(SelectedRequest.this, LoginActivity.class);
                    startActivity(intent);
                    finish();

//                    prefManager.setIsLoggedIn(false);
//                    Intent intent = new Intent(SelectedRequest.this, LoginActivity.class);
//                    startActivity(intent);
//                    finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(SelectedRequest.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if (!SelectedRequest.this.isFinishing() && progress != null && progress.isShowing()) progress.dismiss();
                Toast.makeText(SelectedRequest.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Couldn't connect to the server");
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (!prefManager.isLoggedIn()) {
            Intent intent = new Intent(SelectedRequest.this, LoginActivity.class);
            SelectedRequest.this.startActivity(intent);
            SelectedRequest.super.finish();
        }

        if(prefManager.getFcmrequestId().equals(intent.getStringExtra("request_id")))
            if(intent.hasExtra("source"))
                if(!intent.getStringExtra("source").equals("history")) {
                    if(prefManager.getFcmrequestStatus().equals("canceled"))
                        Log.d(TAG, "The current request seems to have been cancelled");
                    if(prefManager.getFcmrequestStatus().equals("completed"))
                        Log.d(TAG, "The current request seems to have been completed");
                    this.finish();
        }

    }

    @Override
    public void onPause(){
        if (!SelectedRequest.this.isFinishing() && progress != null && progress.isShowing()) progress.dismiss();
        super.onPause();
    }

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (!SelectedRequest.this.isFinishing() && progress != null && progress.isShowing()) progress.dismiss();
        super.onDestroy();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassengerCancelled(PassengerCanceled event) {
        Log.d(TAG, "onPassengerCanceled has been invoked");
        if(event.getRequest_id().equals(intent.getStringExtra("request_id"))){
            if(intent.hasExtra("source"))
                if(!intent.getStringExtra("source").equals("history")) {
                    Log.d(TAG, "The current request seems to have been cancelled");
                    this.finish();
                }
        }
    }

    private void calculate_distance()
    {
        progress = new ProgressDialog(this);
//        progress.setMessage(getString(R.string.FCMRequest_waiting_for_server));
        progress.setMessage("Calculating distance..");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        GoogleDirection.withServerKey(GOOGLE_DIRECTIONS_API)
                .from(pickupPoint)
                .to(destPoint)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        // Do something here
//                        Toast.makeText(MapsActivity.this, "Route successfully computed ", Toast.LENGTH_SHORT).show();
//                        toast.setText("Route successfully computed ");
//                        toast.show();
//                        Log.d(TAG, "showRoute: Route successfully computed ");
//
                        if(!SelectedRequest.this.isFinishing() && progress != null && progress.isShowing())progress.dismiss();

                        if(direction.isOK()) {
//                            // Check if user hasn't cancelled:
//                            if (UIState != UI_STATE.DETAILED){
//                                return;
//                            }

                            // Do
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);


//                            Double priceValue = (double) (Integer.valueOf(distance) *  Integer.valueOf(duration) / 3/60/1000);
//                            ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
//                            PolylineOptions polylineOptions = DirectionConverter.createPolyline(MapsActivity.this, directionPositionList, 5, getResources().getColor(R.color.colorPrimary));
//                            if (routePolyline != null) {
//                                routePolyline.remove();
//                            }
//                            routePolyline = mMap.addPolyline(polylineOptions);
//
//
                            // Distance info
                            Info distanceInfo = leg.getDistance();
//                            Info durationInfo = leg.getDuration();
                            float distance = Float.valueOf(distanceInfo.getValue()) / 1000;
//                            String duration = durationInfo.getValue();
//                            calculatePrice(Integer.valueOf(duration), Integer.valueOf(distance));
                            ((TextView) findViewById(R.id.request_details_distance)).setText(String.format("%s Km", String.valueOf(distance)));

                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something here
//                        toast.setText( "Route Failed ");
//                        toast.show();
//                        showRoute();
//                        Log.d(TAG, "showRoute: Route Failed ");

                        if(!SelectedRequest.this.isFinishing() && progress != null && progress.isShowing())progress.dismiss();

                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverLoggedout(DriverLoggedout event) {
        Log.d(TAG, "onDriverLoggedout has been invoked");
        String lastEmail = prefManager.getLastEmail();
        String lastPassword = prefManager.getLastPassword();
        prefManager.editor.clear().apply();
        prefManager.setLastPassword(lastPassword);
        prefManager.setLastEmail(lastEmail);
        prefManager.setIsLoggedIn(false);
//        prefManager.setExternalLogout(false);
        EventBus.getDefault().post(new UnbindBackgroundLocationService());

        Intent blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);
        stopService(blsIntent);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


}
