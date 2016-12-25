package com.vogella.android.navigationwidgetattempt;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.Wisam.Events.DriverActive;
import com.Wisam.Events.UnbindBackgroundLocationService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.vogella.android.navigationwidgetattempt.BackgroundLocationService.ACCESS_FINE_LOCATION_CODE;
import static com.vogella.android.navigationwidgetattempt.BackgroundLocationService.checkedLocation;
import static com.vogella.android.navigationwidgetattempt.BackgroundLocationService.permissionIsRequested;

public class LocationSettingCallback extends Activity {
    private static final String TAG = LocationSettingCallback.class.getSimpleName();
    protected static Activity activity;
    protected static final int REQUEST_CHECK_SETTINGS = 21314;
    private Boolean mIsBound;
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

/*
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_CODE) {
            Log.d(TAG, "ACCESS_FINE_LOCATION_CODE onRequestPermissionsResult:");
            permissionIsRequested = false;
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "ACCESS_FINE_LOCATION_CODE onRequestPermissionsResult: Granted");
                if(!checkedLocation) {
                    backgroundLocationService.checkLocationSettings();
                    checkedLocation = true;
                }
            } else {
                Toast.makeText(this, "You can't receive requests unless you enable this permission\n" +
                                "if you want to receive requests, press the 'Go active' button below",
                        Toast.LENGTH_LONG).show();
                prefManager.setActive(false);
//                setUI();
                EventBus.getDefault().post(new DriverActive(false));
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }
*/

    @Override
    public void onStart() {
        getApplicationContext().bindService(blsIntent, mConnection, BIND_IMPORTANT);
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        if(mIsBound) {
            try {
                getApplicationContext().unbindService(mConnection);
            }
            catch (java.lang.IllegalArgumentException ignored){
                Log.d(TAG, "onDestroy: unbindService returned exception" + ignored.toString());
            }
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
//        if (!MainActivity.this.isFinishing() && progress != null && progress.isShowing()) progress.dismiss();
        Log.d(TAG, "onDestroy");
        if(mIsBound) {
            try {
                getApplicationContext().unbindService(mConnection);
            }
            catch (java.lang.IllegalArgumentException ignored){
                Log.d(TAG, "onDestroy: unbindService returned exception" + ignored.toString());
            }
            mIsBound = false;
        }
        super.onDestroy();
    }


}
