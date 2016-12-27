package com.vogella.android.navigationwidgetattempt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.UnbindBackgroundLocationService;
import com.Wisam.POJO.RequestsResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HistoryActivity extends AppCompatActivity {

    private static final String DUMMY_REQUEST_ID = "1243";
    private static final String DUMMY_PICKUP = "15.5838046,32.5543825";
    private static final String DUMMY_DEST = "15.8838046, 32.6543825";
    private static final String DUMMY_PASSENGER_NAME = "John Green";
    private static final String DUMMY_PASSENGER_PHONE = "0123456789";
    private static final String DUMMY_STATUS = "on_the_way";
    private static final String DUMMY_NOTES = "Drive slowly";
    private static final String DUMMY_PRICE = "43";
    private static final String DUMMY_TIME = "06/11/2016 - 15:45";
    private static final String TAG = "UbDriver";
    private PrefManager prefManager;
    private RequestAdapter ca;

    private List<request> History = new ArrayList<request>(){
        {
        }
    };
//    private static request current_request = new request(DUMMY_REQUEST_ID, DUMMY_PICKUP, DUMMY_DEST,
//            DUMMY_PASSENGER_NAME, DUMMY_PASSENGER_PHONE, DUMMY_TIME, DUMMY_PRICE, DUMMY_NOTES,
//            DUMMY_STATUS);
    private RecyclerView previous_requests;
    private RecyclerView.Adapter RVadapter;
    private RecyclerView.LayoutManager layoutManager;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefManager = new PrefManager(this);
        setContentView(R.layout.activity_history);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.HistoryActivityGradientShadow).setVisibility(View.GONE);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.history_toolbar);
//        toolbar.setBackgroundColor(getResources().getColor(R.color.white));
//        ((TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.driver_active));
//        ((TextView) findViewById(R.id.toolbar_title)).setTextColor(getResources().getColor(R.color.colorPrimary));
//        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
//        toolbar.setTitle(R.string.history);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        previous_requests = (RecyclerView) findViewById(R.id.past_requests);
        previous_requests.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        previous_requests.setLayoutManager(layoutManager);
//        RequestAdapter ca = new RequestAdapter(createList(30));
        ca = new RequestAdapter(History, this);
        previous_requests.setAdapter(ca);
        serverRequest();
//        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ongoing_request);
//        relativeLayout.setVisibility(View.INVISIBLE);


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

    private List<request> createList(int size) {

        List<request> result = new ArrayList<request>();
        for (int i=1; i <= size; i++) {
            request ci = new request();
            ci.setPassenger_name(DUMMY_PASSENGER_NAME + i);
            ci.setPassenger_phone(DUMMY_PASSENGER_PHONE + i);
            ci.setStatus(DUMMY_STATUS);
            ci.setTime(DUMMY_TIME);
            ci.setDestString(DUMMY_DEST);
//            ci.dest[0] = Double.parseDouble(DUMMY_DEST.split(",")[0]);
//            ci.dest[1] = Double.parseDouble(DUMMY_DEST.split(",")[1]);
            ci.setPickupString(DUMMY_PICKUP);
//            ci.pickup[0] = Double.parseDouble(DUMMY_PICKUP.split(",")[0]);
//            ci.pickup[1] = Double.parseDouble(DUMMY_PICKUP.split(",")[1]);
//            ci.pickup = DUMMY_PICKUP;
            ci.setPrice(DUMMY_PRICE + i);

            result.add(ci);

        }

        return result;
    }


    private void serverRequest() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
              String email = prefManager.pref.getString("UserEmail","");
        String password = prefManager.pref.getString("UserPassword","");

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.FCMRequest_waiting_for_server));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        RestService service = retrofit.create(RestService.class);
        Call<RequestsResponse> call = service.requests("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP));
        call.enqueue(new Callback<RequestsResponse>() {
            @Override
            public void onResponse(Call<RequestsResponse> call, Response<RequestsResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if(!HistoryActivity.this.isFinishing() && progress != null && progress.isShowing())
                    progress.dismiss();
                if (response.isSuccess() && response.body() != null){
                    List <request> rides = response.body().getRides();
                    List <request> history = new ArrayList<request>(){{}};
                    for (request i : rides){
                        if (i.getStatus().equals("completed") || i.getStatus().equals("canceled") || i.getStatus().equals("missed")) {
                            long unixTime;
                            if(i.getTime().equals("now"))
                                unixTime = System.currentTimeMillis();
                            else {
                                Log.d(TAG,"Time is :" + i.getTime());
                                unixTime = Long.valueOf(i.getTime()) * 1000; // In this case, the server sends the time in seconds while unix time needs milliseconds
                            }
                            i.setTime(String.valueOf(DateUtils.getRelativeTimeSpanString(unixTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)));
//                            Date df = new java.util.Date(unixTime);
//                            SimpleDateFormat sdf = new SimpleDateFormat("dd MM, yyyy hh:mma");
//                            sdf.setTimeZone(TimeZone.getTimeZone("Africa/Khartoum"));
//                            i.setTime(sdf.format(df));

                            i.setPrice(i.getPrice() + " SDG");
                            history.add(0, i);
                        }
                    }
                    if(history.size() == 0)
                        Toast.makeText(HistoryActivity.this, R.string.no_previous_requests,Toast.LENGTH_LONG).show();
                    HistoryActivity.this.setRequestsList(history);
                } else if (response.code() == 401){
                    Toast.makeText(HistoryActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onResponse: User not logged in");
                   logout();

//                    prefManager.setIsLoggedIn(false);
//                    Intent intent = new Intent(HistoryActivity.this, LoginActivity.class);
//                    startActivity(intent);
//                    finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(HistoryActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<RequestsResponse> call, Throwable t) {
                if(!HistoryActivity.this.isFinishing() && progress != null && progress.isShowing())
                    progress.dismiss();
                Toast.makeText(HistoryActivity.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void setRequestsList(List<request> rides) {
//        if (History.isEmpty()){
            History = rides;
            ca.updateRequestsList(History);
            ca.notifyDataSetChanged();
//        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverLoggedout(DriverLoggedout event) {
        Log.d(TAG,"onDriverLoggedout invoked");
        logout();
    }

    private void logout() {
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

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
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
        if (!HistoryActivity.this.isFinishing() & progress != null && progress.isShowing()) progress.dismiss();
        super.onDestroy();
    }


}