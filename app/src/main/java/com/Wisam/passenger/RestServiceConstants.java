package com.Wisam.passenger;

import android.content.Context;

/**
 * Created by nezuma on 11/19/16.
 */
public class RestServiceConstants {
    static final int REQUEST_CHECK_SETTINGS = 21314;
    static String goActiveID = "goActiveID";

    public String getBaseUrl(Context context){
        PrefManager prefManager = new PrefManager(context);
        return prefManager.getBaseUrl();
    }

}
