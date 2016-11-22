package com.vogella.android.navigationwidgetattempt;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.POJO.AcceptResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FCMRequest extends AppCompatActivity {

    private static final String TAG = "FCMRequest";
    request request = new request();
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcmrequest);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar_timerview);
//        Animation an = new RotateAnimation(0.0f, 90.0f, 250f, 273f);
//        an.setFillAfter(true);
//        progressBar.startAnimation(an);

        prefManager = new PrefManager(this);

        Intent data = getIntent();
        request.passenger_name = data.getStringExtra("passenger_name");
        request.passenger_phone = data.getStringExtra("passenger_phone");
        request.status = "Accepted";
        request.time = data.getStringExtra("time");
        request.notes = data.getStringExtra("notes");
        request.price = data.getStringExtra("price");
        request.request_id = data.getExtras().getString("request_id");
        request.pickup[0] = Double.parseDouble(data.getStringExtra("pickup").split(",")[0]);
        request.pickup[1] = Double.parseDouble(data.getStringExtra("pickup").split(",")[1]);
//        request.pickup = data.getStringExtra("pickup");
        request.dest[0] = Double.parseDouble(data.getStringExtra("dest").split(",")[0]);
        request.dest[1] = Double.parseDouble(data.getStringExtra("dest").split(",")[1]);

        ((TextView) findViewById(R.id.fcmrequest_pickup)).setText(data.getStringExtra("pickup"));
        ((TextView) findViewById(R.id.fcmrequest_price)).setText(request.price);
        ((TextView) findViewById(R.id.fcmrequest_time)).setText(request.time);
        CountDownTimer countDownTimer = new CountDownTimer(20 * 1000, 500) {
            @Override
            public void onTick(long l) {
                long seconds = l / 1000;
                progressBar.setProgress((int)seconds);
            }

            @Override
            public void onFinish() {
                Toast.makeText(getBaseContext(),"You didn't accept", Toast.LENGTH_LONG).show();
            }
        }.start();
        progressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serverRequest("11111", true);

//                Intent intent = new Intent(getBaseContext(), MainActivity.class);
  //              startActivity(intent);
                FCMRequest.super.finish();
            }
        });

        ((TextView) findViewById(R.id.fcmrequest_reject)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serverRequest("11111", false);
                FCMRequest.super.finish();
            }
        });

    }
    private void serverRequest(String request_id, final boolean accepted ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = "";
        String password = "";

        RestService service = retrofit.create(RestService.class);
        Call<AcceptResponse> call = service.accept("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                                                    request_id,accepted);
        call.enqueue(new Callback<AcceptResponse>() {
            @Override
            public void onResponse(Call<AcceptResponse> call, Response<AcceptResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    if(accepted) {
                        if(response.body().getStatus() == 0) {
                            Toast.makeText(getBaseContext(), "You have accepted this request " +
                                    "You can now view it in the ongoing requests tab", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "You have accepted the request");
                            OngoingRequestsActivity.addRequest(request);
                        }
                        else if(response.body().getStatus() == 3){
                             Toast.makeText(getBaseContext(), "Another driver has alread accepted this request",
                                     Toast.LENGTH_LONG).show();
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

            }

            @Override
            public void onFailure(Call<AcceptResponse> call, Throwable t) {

            }
        });
    }


}
