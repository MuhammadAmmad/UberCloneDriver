package com.vogella.android.navigationwidgetattempt;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.ChangeActiveUpdateInterval;
import com.Wisam.Events.UnbindBackgroundLocationService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.vogella.android.navigationwidgetattempt.BackgroundLocationService.mRequestingLocationUpdates;
import static com.vogella.android.navigationwidgetattempt.MainActivity.ACTIVE_NOTIFICATION_ID;

public class PopupActivity extends AppCompatActivity {
    private BackgroundLocationService backgroundLocationService;
    private Boolean mIsBound = false;
    private PrefManager prefManager;
    protected static final int UPDATE_DURING_REQUEST = 10 * 1000; //10 seconds
    protected static final int UPDATE_WHILE_IDLE = 5 * 1000;//2 * 60 * 1000;
    private ServiceConnection mConnection;
    private static final String TAG = PopupActivity.class.getSimpleName();
    private boolean enablingLocation = false;
    private Intent blsIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_popup);
        prefManager = new PrefManager(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.popup_toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
        toolbar.setBackgroundColor(getResources().getColor(R.color.white));
        toolbar.setTitle("Passenger");
//        toolbar.setNavigationIcon(R.drawable.mini_logo);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setIcon(R.drawable.mini_logo);


        // This is called when the connection with the service has been
// established, giving us the service object we can use to
// interact with the service.  Because we have bound to a explicit
// service that we know is running in our own process, we can
// cast its IBinder to a concrete class and directly access it.
// Tell the user about this for our demo.
//            Toast.makeText(Binding.this, R.string.local_service_connected,
//                    Toast.LENGTH_SHORT).show();
// This is called when the connection with the service has been
// unexpectedly disconnected -- that is, its process crashed.
// Because it is running in our same process, we should never
// see this happen.
//            Toast.makeText(Binding.this, R.string.local_service_disconnected,
//                    Toast.LENGTH_SHORT).show();
        prefManager.setActive(false);

        mConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  Because we have bound to a explicit
                // service that we know is running in our own process, we can
                // cast its IBinder to a concrete class and directly access it.
                Log.d(TAG, "onServiceConnected");
                backgroundLocationService = ((BackgroundLocationService.LocalBinder) service).getServerInstance();
                mIsBound = true;

                // Tell the user about this for our demo.
                //            Toast.makeText(Binding.this, R.string.local_service_connected,
                //                    Toast.LENGTH_SHORT).show();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                // Because it is running in our same process, we should never
                // see this happen.
                Log.d(TAG, "onServiceDisconnected");
                backgroundLocationService = null;
                mIsBound = false;

                //            Toast.makeText(Binding.this, R.string.local_service_disconnected,
                //                    Toast.LENGTH_SHORT).show();
            }
        };
        blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);

        getApplicationContext().bindService(blsIntent, mConnection, BIND_IMPORTANT);

        mIsBound = true;

        ((TextView) findViewById(R.id.enable_location)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"enable location clicked");
                enablingLocation = true;
                backgroundLocationService.checkLocationSettings();
                final ProgressDialog progress = new ProgressDialog(PopupActivity.this);
                progress.setMessage(getString(R.string.updating_request_status));
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setIndeterminate(true);
                progress.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if(mRequestingLocationUpdates){
                            if (!PopupActivity.this.isFinishing() && progress.isShowing()) progress.dismiss();
//                            setAlarms();
                            EventBus.getDefault().post(new ChangeActiveUpdateInterval());
                            backgroundLocationService.checkLocation();
                            finish();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"Couldn't get your location. Go to the app and try again",Toast.LENGTH_LONG).show();
                        }
                    }
                }, 4000);

            }
        });
        ((TextView) findViewById(R.id.become_inactive)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"become inactive clicked");
                if(mIsBound) {
                    getApplicationContext().unbindService(mConnection);
                    mIsBound = false;
                }
                prefManager.setActive(false);
//                Log.d(TAG,"Posting new UnbindBackgroundLocationService");
//                EventBus.getDefault().post(new UnbindBackgroundLocationService());
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopService(blsIntent);
                        handler.removeCallbacksAndMessages(null);
                        finish();
                    }
                }, 200);
            }
        });



    }

    @Override
    protected void onDestroy() {
//        if (!MainActivity.this.isFinishing() && progress != null && progress.isShowing()) progress.dismiss();
        Log.d(TAG,"onDestroy");
        if(enablingLocation) {
            Log.d(TAG,"Decided to enable location");
            if (mIsBound) {
                try {
                    getApplicationContext().unbindService(mConnection);
                } catch (java.lang.IllegalArgumentException ignored) {
                    Log.d(TAG, "onDestroy: unbindService returned exception" + ignored.toString());
                }
                mIsBound = false;
            }
        }
        else{
            Log.d(TAG,"Decided not to enable location");
            if(mIsBound) {
                getApplicationContext().unbindService(mConnection);
                mIsBound = false;
            }
            prefManager.setActive(false);
//            Log.d(TAG,"Posting new UnbindBackgroundLocationService");
//            EventBus.getDefault().post(new UnbindBackgroundLocationService());
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"onDestroy: stopping BackgroundLocationService");
                    stopService(blsIntent);
                    handler.removeCallbacksAndMessages(null);
                }
            }, 200);
        }
        super.onDestroy();
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


    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onUnbindBackgroundLocationService(UnbindBackgroundLocationService event) {
        Log.d(TAG, "onUnbindBackgroundLocationService has been invoked");
        if(mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
    }



}

