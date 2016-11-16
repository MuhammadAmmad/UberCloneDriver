package com.vogella.android.navigationwidgetattempt;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FCMRequest extends AppCompatActivity {

    request request = new request();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcmrequest);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar_timerview);
//        Animation an = new RotateAnimation(0.0f, 90.0f, 250f, 273f);
//        an.setFillAfter(true);
//        progressBar.startAnimation(an);
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
                Toast.makeText(getBaseContext(),"You have accepted this request " +
                        "You can now view it in the ongoing requests tab", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                OngoingRequestsActivity.addRequest(request);
                startActivity(intent);
            }
        });

    }
}
