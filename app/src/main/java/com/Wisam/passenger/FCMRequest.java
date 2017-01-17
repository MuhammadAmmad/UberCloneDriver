package com.Wisam.passenger;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.DriverLoggedout;
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


public class FCMRequest extends AppCompatActivity {

    private static final String TAG = "FCMRequest";
    private static final int NOT_TIMEOUT = 0;
    request request = new request();
    private PrefManager prefManager;
    private static final int TIMEOUT = 1;
    private static int reason = TIMEOUT;
    private PowerManager.WakeLock wl;
    private Intent data;
    private LatLng pickupPoint;
    private LatLng driverLocation;
    private Ringtone r;
    private ProgressDialog progress;
    private static final String GOOGLE_DIRECTIONS_API = "AIzaSyDpJmpRN0BxJ76X27K0NLTGs-gDHQtoxXQ";
    private boolean waitForResopnse = false;
    private boolean finishOnResponse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
        wl.acquire();
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        data = getIntent();


        prefManager = new PrefManager(this);
        request.setPassenger_name(data.getStringExtra("passenger_name"));
        request.setPassenger_phone(data.getStringExtra("passenger_phone"));
        request.setStatus("accepted");
        request.setTime(data.getStringExtra("time"));
        request.setNotes(data.getStringExtra("notes"));
        request.setPrice(data.getStringExtra("price"));
        request.setRequest_id(data.getExtras().getString("request_id"));
        request.setPickupText(data.getStringExtra("pickup_text"));
        request.setPickupString(data.getStringExtra("pickup"));
        request.setDestText(data.getStringExtra("dest_text"));
        request.setDestString(data.getStringExtra("dest"));
        long unixTime;
        if(request.getTime().equals("now")) {
            unixTime = System.currentTimeMillis();
        }
        else {
            Log.d(TAG, "Time is :" + request.getTime());
            unixTime = Long.valueOf(request.getTime()); // In this case, the server sends the time in milliseconds just as expected from unixTime.
            request.setTime(String.valueOf(DateUtils.getRelativeTimeSpanString(unixTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)));
        }

        pickupPoint = new LatLng(request.getPickup()[0],request.getPickup()[1]);
        driverLocation = new LatLng(Double.parseDouble(prefManager.getCurrentLocation().split(",")[0]),
                Double.parseDouble(prefManager.getCurrentLocation().split(",")[1]));



        Uri tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        r = RingtoneManager.getRingtone(getApplicationContext(), tone);
        r.play();

        setContentView(R.layout.activity_fcmrequest);
        calculate_distance();
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar_timerview);
        ((TextView) findViewById(R.id.fcmrequest_pickup)).setText(request.getPickupText());
        ((TextView) findViewById(R.id.fcmrequest_price)).setText(String.format(getString(R.string.new_request_price), request.getPrice()));
        ((TextView) findViewById(R.id.fcmrequest_time)).setText(request.getTime());
        ((TextView) findViewById(R.id.fcmrequest_distance)).setText(R.string.new_request_distance);
        final CountDownTimer countDownTimer = new CountDownTimer(30 * 1000, 500) {
            @Override
            public void onTick(long l) {
                long seconds = l / 1000;
                progressBar.setProgress((int) seconds);
            }

            @Override
            public void onFinish() {
                if (!FCMRequest.this.isFinishing()) {
                    if (!waitForResopnse) {
                        r.stop();
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FCMRequest.this);
                        alertBuilder.setCancelable(false);
                        alertBuilder.setMessage(getString(R.string.fcm_ignored));
                        alertBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FCMRequest.super.finish();
                            }
                        });
                        alertBuilder.show();
//                    Toast.makeText(getBaseContext(), R.string.fcm_ignored, Toast.LENGTH_LONG).show();
                    } else {
                        finishOnResponse = true;
                    }
                }
            }
        }.start();

        ((LinearLayout) findViewById(R.id.activity_fcmrequest)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                r.stop();
//                countDownTimer.cancel();
                serverAccept(request.getRequest_id(), 1);
            }
        });

        ((TextView) findViewById(R.id.fcmrequest_reject)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                r.stop();
//                countDownTimer.cancel();
                serverAccept(request.getRequest_id(), 0);
            }
        });
    }

    private void serverAccept(String request_id, final int accepted ) {
        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(FCMRequest.this))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
              String email = prefManager.pref.getString("UserEmail","");
        String password = prefManager.pref.getString("UserPassword","");

        waitForResopnse = true;

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.FCMRequest_waiting_for_server));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();


        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.accept("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                                                    request_id,accepted);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if(!FCMRequest.this.isFinishing() && progress != null && progress.isShowing())progress.dismiss();
                if (response.isSuccess() && response.body() != null){
                    if(accepted == 1) {
                        if(response.body().getStatus() == 0) {
                            Log.d(TAG, "You have accepted the request");
//                            Toast.makeText(getBaseContext(), R.string.FCM_accepted_new_request, Toast.LENGTH_LONG).show();
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FCMRequest.this);
                            alertBuilder.setMessage(getString(R.string.FCM_accepted_new_request));
                            alertBuilder.setCancelable(false);
                            alertBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(data.getStringExtra("time").equals("now")) {
                                        Intent intent = new Intent(FCMRequest.this, SelectedRequest.class);
                                        intent.putExtras(data);
                                        intent.putExtra("status", "on_the_way");
                                        intent.putExtra("source", "FCMRequest");
                                        startActivity(intent);
                                        waitForResopnse = false;
                                    }
                                    FCMRequest.super.finish();}
                            });
                            alertBuilder.show();
                        }
                        else if(response.body().getStatus() == 3){
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FCMRequest.this);
                            alertBuilder.setCancelable(false);
                            alertBuilder.setMessage(getString(R.string.FCM_another_driver_accepted));
                            alertBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    waitForResopnse = false;
                                    FCMRequest.super.finish();
                                }
                            });
                            alertBuilder.show();
                        }
                    }
                    else {
                        Log.d(TAG, "You have rejected the request");
//                        Toast.makeText(getBaseContext(), R.string.FCM_rejected_request, Toast.LENGTH_LONG).show();
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FCMRequest.this);
                        alertBuilder.setMessage(getString(R.string.FCM_rejected_request));
                        alertBuilder.setCancelable(false);
                        alertBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                waitForResopnse = false;
                                FCMRequest.super.finish();
                            }
                        });
                        alertBuilder.show();
                    }
                } else if (response.code() == 401){
//                    Toast.makeText(FCMRequest.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Accept request onResponse: User not logged in");
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FCMRequest.this);
                    alertBuilder.setMessage(getString(R.string.authorization_error));
                    alertBuilder.setCancelable(false);
                    alertBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            waitForResopnse = false;
                            logout();
                        }
                    });
                    alertBuilder.show();
                } else {
//                    Toast.makeText(FCMRequest.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FCMRequest.this);
                    alertBuilder.setMessage(getString(R.string.server_unknown_error));
                    alertBuilder.setCancelable(false);
                    alertBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            waitForResopnse = false;
                            if(finishOnResponse){
                                finishOnResponse = false;
                                FCMRequest.super.finish();
                            }
                        }
                    });
                    alertBuilder.show();
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                Log.d(TAG, "The response is onFailure");
                if(!FCMRequest.this.isFinishing() && progress != null && progress.isShowing())progress.dismiss();
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(FCMRequest.this);
                alertBuilder.setMessage(getString(R.string.server_timeout));
                alertBuilder.setCancelable(false);
                alertBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        waitForResopnse = false;
                        if(finishOnResponse){
                            finishOnResponse = false;
                            FCMRequest.super.finish();
                        }
                    }
                });
                alertBuilder.show();
            }
        });
    }

    private void calculate_distance()
    {
        GoogleDirection.withServerKey(GOOGLE_DIRECTIONS_API)
                .from(pickupPoint)
                .to(driverLocation)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if(direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);

                            Info distanceInfo = leg.getDistance();
                            float distance = Float.valueOf(distanceInfo.getValue()) / 1000;
                            ((TextView) findViewById(R.id.fcmrequest_distance)).setText(String.format("%s Km", String.valueOf(distance)));
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something here
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!prefManager.isLoggedIn()) {
            logout();
        }
    }

    @Override
    protected void onDestroy() {
        if (!FCMRequest.this.isFinishing() && progress != null && progress.isShowing()) progress.dismiss();
        r.stop();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }


    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
//        onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverLoggedout(DriverLoggedout event) {
        Log.d(TAG, "onDriverLoggedout has been invoked");
        logout();
    }

    private void logout() {
        String lastEmail = prefManager.getLastEmail();
        String lastPassword = prefManager.getLastPassword();
        prefManager.editor.clear().apply();
        prefManager.setLastPassword(lastPassword);
        prefManager.setLastEmail(lastEmail);
        prefManager.setIsLoggedIn(false);

        EventBus.getDefault().post(new UnbindBackgroundLocationService());

        Intent blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);
        stopService(blsIntent);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
