package com.Wisam.passenger;

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
import android.widget.Toast;

import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.PassengerArrived;
import com.Wisam.Events.PassengerCanceled;
import com.Wisam.Events.UnbindBackgroundLocationService;
import com.Wisam.POJO.RequestsResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OngoingRequestsActivity extends AppCompatActivity{

    private static final String DUMMY_REQUEST_ID = "1243";
    private static final String DUMMY_PICKUP = "15.5838046,32.5543825";
    private static final String DUMMY_DEST = "15.8838046, 32.6543825";
    private static final String DUMMY_PASSENGER_NAME = "John Green";
    private static final String DUMMY_PASSENGER_PHONE = "0123456789";
    private static final String DUMMY_STATUS = "on_the_way";
    private static final String DUMMY_NOTES = "Drive slowly";
    private static final String DUMMY_PRICE = "43";
    private static final String DUMMY_TIME = "06/11/2016 ; 15:45";
    private static final String TAG = "OngoingRequestsActivity";
    private static final String DUMMY_DEST_TEXT = "My home";
    private static final String DUMMY_PICKUP_TEXT = "My workplace";
    private static final int FINISH_PARENT = 7;
    private RecyclerView previous_requests;
    private RecyclerView.Adapter RVadapter;
    private RecyclerView.LayoutManager layoutManager;
    private  OngoingRequestAdapter ca;
    private static List<request> requestList = new ArrayList<request>(){{}};
    private static boolean initialized = false;
    private PrefManager prefManager;

    private static final int SELECTED_REQUEST_CODE = 43542;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_requests);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.ongoingRequestsActivityGradientShadow).setVisibility(View.GONE);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.incoming_toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        previous_requests = (RecyclerView) findViewById(R.id.future_requests);
        previous_requests.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        previous_requests.setLayoutManager(layoutManager);
        requestList = new ArrayList<request>(){
            {
            }
        };
        ca = new OngoingRequestAdapter(requestList, this);
        previous_requests.setAdapter(ca);
        prefManager = new PrefManager(this);
        // Server request
        getOngoingRequests();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,String.format("onActivityResult: requestCode = %d, resultCode = %d", requestCode, resultCode));
        if (requestCode == SELECTED_REQUEST_CODE && resultCode == RESULT_OK) {
            if(data.hasExtra("request_id")) {
                removeRequest(data.getStringExtra("request_id"));
                ca.notifyDataSetChanged();
            }
        }
     }

    private void getOngoingRequests() {
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();
        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(OngoingRequestsActivity.this))
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        //showProgress()

        String email = prefManager.pref.getString("UserEmail","");
        String password = prefManager.pref.getString("UserPassword","");

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.FCMRequest_waiting_for_server));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();


        RestService service = retrofit.create(RestService.class);
        Call<RequestsResponse> call = service.requests("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP));
        call.enqueue(new Callback<RequestsResponse>() {
            @Override
            public void onResponse(Call<RequestsResponse> call, Response<RequestsResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                Log.d(TAG, "onResponse: code: " + response.code());
                if(!OngoingRequestsActivity.this.isFinishing() && progress != null && progress.isShowing())
                    progress.dismiss();
                if (response.isSuccess() && response.body() != null){
                    List <request> rides = response.body().getRides();
                    List <request> upcoming = new ArrayList<request>(){{}};
                    for (request i : rides){
                        if (i.getStatus().equals("accepted")) {
                            long unixTime;
                            if(i.getTime().equals("now"))
                                i.setTime("Now");
                            else {
                                Log.d(TAG, "Time is :" + i.getTime());
                                unixTime = Long.valueOf(i.getTime()) * 1000; // In this case, the server sends the time in seconds while unix time needs milliseconds
                                i.setTime(String.valueOf(DateUtils.getRelativeTimeSpanString(unixTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)));
                                i.setPrice(i.getPrice() + " SDG");
                            }
                            upcoming.add(0, i);
                        }
                    }
                    OngoingRequestsActivity.this.setRequestsList(upcoming);
                    if(upcoming.size() == 0)
                        Toast.makeText(OngoingRequestsActivity.this, R.string.no_incoming_requests,Toast.LENGTH_LONG).show();
                } else if (response.code() == 401){
                    Toast.makeText(OngoingRequestsActivity.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    String lastEmail = prefManager.getLastEmail();
                    String lastPassword = prefManager.getLastPassword();
                    prefManager.editor.clear().apply();
                    prefManager.setLastPassword(lastPassword);
                    prefManager.setLastEmail(lastEmail);
                    prefManager.setIsLoggedIn(false);
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    Intent blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);
                    stopService(blsIntent);

                    EventBus.getDefault().post(new DriverLoggedout());

                    Intent intent = new Intent(OngoingRequestsActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(OngoingRequestsActivity.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<RequestsResponse> call, Throwable t) {
                if(!OngoingRequestsActivity.this.isFinishing() && progress != null && progress.isShowing())
                    progress.dismiss();
                Toast.makeText(OngoingRequestsActivity.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setRequestsList(List<request> rides) {
        requestList = rides;
        ca.updateRequestsList(requestList);
        ca.notifyDataSetChanged();
    }


    public static boolean addRequest(request request){
        requestList.add(requestList.size(),request);
        return true;
    }
    public boolean removeRequest(String request_id){
        int i;
        for (i = 0; i < requestList.size(); i++) {
            if (requestList.get(i).getRequest_id().equals(request_id)) {
                requestList.remove(requestList.get(i));
                Log.d(TAG, "The request " + request_id + " has been removed");
                ca.notifyDataSetChanged();
                break;
            }
        }
        return true;
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

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriverLoggedout(DriverLoggedout event) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassengerCancelled(PassengerCanceled event) {
        Log.d(TAG, "onPassengerCanceled has been invoked");
        removeRequest(event.getRequest_id());
    }

    //Most likely not needed.
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPassengerArrived(PassengerArrived event) {
        Log.d(TAG, "onPassengerArrived has been invoked");
        removeRequest(event.getRequest_id());
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
        else {
            if (!prefManager.getFcmrequestId().equals("No data")) {
                if (prefManager.getFcmrequestStatus().equals("canceled"))
                    Log.d(TAG, "The current request seems to have been cancelled");
                if (prefManager.getFcmrequestStatus().equals("completed"))
                    Log.d(TAG, "The current request seems to have been completed");
                removeRequest(prefManager.getFcmrequestId());
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (!OngoingRequestsActivity.this.isFinishing() & progress != null && progress.isShowing()) progress.dismiss();
        super.onDestroy();
    }

}
