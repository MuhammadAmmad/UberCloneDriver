package com.Wisam.passenger;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;

import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.UnbindBackgroundLocationService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LocationSettingCallback extends Activity {
    private static final String TAG = LocationSettingCallback.class.getSimpleName();
    protected static Activity activity;
    protected static final int REQUEST_CHECK_SETTINGS = 21314;
    private Boolean mIsBound = false;
    private BackgroundLocationService backgroundLocationService;
    private PrefManager prefManager;
    private ServiceConnection mConnection;
    private Intent blsIntent;
    private boolean handleWhenBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_setting_callback);
        activity = this;
        prefManager = new PrefManager(this);
        Log.d(TAG, "onCreate");

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
                if(handleWhenBound){
                    backgroundLocationService.startLocationUpdates();
                    finish();
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                // Because it is running in our same process, we should never
                // see this happen.
                Log.d(TAG, "onServiceDisconnected");
                backgroundLocationService = null;
                mIsBound = false;
            }
        };
        blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                case RESULT_OK:
                    Log.i(TAG, "User agreed to make required location settings changes.");
                    if(backgroundLocationService != null) {
                        backgroundLocationService.startLocationUpdates();
                        finish();
                    }
                    else {
                        handleWhenBound = true;
                    }
                    break;
                case RESULT_CANCELED:
                    Log.i(TAG, "User chose not to make required location settings changes.");
                    prefManager.setActive(false);
                    if(mIsBound) {
                        getApplicationContext().unbindService(mConnection);
                        mIsBound = false;
                    }
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    stopService(blsIntent);
                    finish();
                    break;
            }
        }

    }

    @Override
    public void onStart() {
        getApplicationContext().bindService(blsIntent, mConnection, BIND_IMPORTANT);
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        if(mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
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

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if(mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
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
        if(mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
        EventBus.getDefault().post(new UnbindBackgroundLocationService());
        Intent blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);
        stopService(blsIntent);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    public void onResume() {
        Log.d(TAG,"onResume:");
        super.onResume();
        if (!prefManager.isLoggedIn()) {
            logout();
        }
    }
}
