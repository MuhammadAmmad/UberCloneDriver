package com.vogella.android.navigationwidgetattempt;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.POJO.StatusResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent data = getIntent();
        setContentView(R.layout.activity_fcmrequest);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar_timerview);
//        Animation an = new RotateAnimation(0.0f, 90.0f, 250f, 273f);
//        an.setFillAfter(true);
//        progressBar.startAnimation(an);

                prefManager = new PrefManager(this);
                request.setPassenger_name(data.getStringExtra("passenger_name"));
                request.setPassenger_phone(data.getStringExtra("passenger_phone"));
                request.setStatus("Accepted");
                request.setTime(data.getStringExtra("time"));
                request.setNotes(data.getStringExtra("notes"));
                request.setPrice(data.getStringExtra("price"));
                request.setRequest_id(data.getExtras().getString("request_id"));
                request.setPickupString(data.getStringExtra("pickup"));
//                request.pickup[0] = Double.parseDouble(data.getStringExtra("pickup").split(",")[0]);
//                request.pickup[1] = Double.parseDouble(data.getStringExtra("pickup").split(",")[1]);
//        request.pickup = data.getStringExtra("pickup");
                request.setDestString(data.getStringExtra("dest"));
//                request.dest[0] = Double.parseDouble(data.getStringExtra("dest").split(",")[0]);
//                request.dest[1] = Double.parseDouble(data.getStringExtra("dest").split(",")[1]);
                long unixTime;
                if(request.getTime().equals("now"))
                    unixTime = System.currentTimeMillis();
                else {
                    Log.d(TAG,"Time is :" + request.getTime());
                    unixTime = Long.valueOf(request.getTime());
                }
                Date df = new java.util.Date(unixTime);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MM, yyyy hh:mma");
                sdf.setTimeZone(TimeZone.getTimeZone("Africa/Khartoum"));
                request.setTime(sdf.format(df));

                ((TextView) findViewById(R.id.fcmrequest_pickup)).setText(data.getStringExtra("pickup"));
                ((TextView) findViewById(R.id.fcmrequest_price)).setText(request.getPrice());
                ((TextView) findViewById(R.id.fcmrequest_time)).setText(request.getTime());
                CountDownTimer countDownTimer = new CountDownTimer(20 * 1000, 500) {
                    @Override
                    public void onTick(long l) {
                        long seconds = l / 1000;
                        progressBar.setProgress((int) seconds);
                    }

                    @Override
                    public void onFinish() {
                        if(reason == TIMEOUT)
                            Toast.makeText(getBaseContext(), "You didn't accept", Toast.LENGTH_LONG).show();
                    }
                }.start();
                progressBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        serverAccept(request.getRequest_id(), true);
                        FCMRequest.super.finish();
//                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        //              startActivity(intent);
                    }
                });

                ((TextView) findViewById(R.id.fcmrequest_reject)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        serverAccept(request.getRequest_id(), false);
                        FCMRequest.super.finish();
                    }
                });
    }

    private void serverAccept(String request_id, final boolean accepted ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
              String email = prefManager.pref.getString("UserEmail","");
        String password = prefManager.pref.getString("UserPassword","");

        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.accept("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                                                    request_id,accepted);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    if(accepted) {
                        if(response.body().getStatus() == 0) {
                            Toast.makeText(getBaseContext(), "You have accepted this request " +
                                    "You can now view it in the ongoing requests tab", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "You have accepted the request");
                            OngoingRequestsActivity.addRequest(request);
                            FCMRequest.super.finish();
                        }
                        else if(response.body().getStatus() == 3){
                             Toast.makeText(getBaseContext(), "Another driver has alread accepted this request",
                                     Toast.LENGTH_LONG).show();
                            FCMRequest.super.finish();
                        }
                    }
                    else {
                        Log.d(TAG, "You have rejected the request");
                    }
                } else if (response.code() == 401){
                    Toast.makeText(FCMRequest.this, "Please login to continue", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(FCMRequest.this, LoginActivity.class);
                    FCMRequest.this.startActivity(intent);
                    FCMRequest.super.finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(FCMRequest.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                }
                reason = NOT_TIMEOUT;
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {

            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        if (prefManager.isLoggedIn() == false) {
            Intent intent = new Intent(FCMRequest.this, LoginActivity.class);
            FCMRequest.this.startActivity(intent);
            FCMRequest.super.finish();
        }
    }


}
