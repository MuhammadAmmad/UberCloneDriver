package com.Wisam.passenger;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.UnbindBackgroundLocationService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "Profile Activity";
    public driver driver;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.profileGradientShadow).setVisibility(View.GONE);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        prefManager = new PrefManager(this);

        if (prefManager.isLoggedIn()) {
            driver = prefManager.getDriver();
            TextView fullname = (TextView) findViewById(R.id.profile_fullname);
            TextView phone = (TextView) findViewById(R.id.profile_phone);
            TextView email = (TextView) findViewById(R.id.profile_email);
            TextView gender = (TextView) findViewById(R.id.profile_gender);

            if (fullname != null) {
                fullname.setText(driver.getUsername());
            }
            if (phone != null) {
                phone.setText(driver.getPhone());
            }
            if (email != null) {
                email.setText(driver.getEmail());
            }
            if (gender != null) {
                gender.setText(driver.getGender());
            }

        } else {
            EventBus.getDefault().post(new DriverLoggedout());
        }

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
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogoutRequest(DriverLoggedout logoutRequest){
        Log.d(TAG,"onDriverLoggedout has been invoked");
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
    public void onResume() {
        Log.d(TAG,"onResume:");
        super.onResume();
        if (!prefManager.isLoggedIn()) {
            logout();
        }
    }

}