package com.vogella.android.navigationwidgetattempt;


import android.content.Context;
import android.content.SharedPreferences;


import com.Wisam.POJO.User;
import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by islam on 9/27/16.
 */
public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "main_pref";

    private static final String IS_FIRST_TIME_LAUNCHED = "IsFirstTimeLaunch";
    private static final String IS_LOGGED_IN = "IsLoggedIn";
    private static final String DOING_REQUEST = "DOING_REQUEST";

    private static final String REQUEST_ID = "REQUEST_ID";
    private static final String REQUEST_STATUS = "REQUEST_STATUS";
    private static final String REQUEST_NOTES = "REQUEST_NOTES";
    private static final String REQUEST_TIME = "REQUEST_TIME";
    private static final String REQUEST_PRICE = "REQUEST_PRICE";
    private static final String REQUEST_PASSENGER_NAME = "REQUEST_PASSENGER_NAME";
    private static final String REQUEST_PASSENGER_PHONE = "REQUEST_PASSENGER_PHONE";
    private static final String REQUEST_PICKUP = "REQUEST_PICKUP";
    private static final String REQUEST_PICKUP_TEXT = "REQUEST_PICKUP_TEXT";
    private static final String REQUEST_PICKUP_STRING = "REQUEST_PICKUP_STRING";
    private static final String REQUEST_DEST = "REQUEST_DEST";
    private static final String REQUEST_DEST_TEXT = "REQUEST_DEST_TEXT";
    private static final String REQUEST_DEST_STRING = "REQUEST_DEST_STRING";

    // User data
    private static final String USER_FULLNAME = "UserName";
    private static final String USER_PASSWORD = "UserPassword";
    private static final String USER_EMAIL = "UserEmail";
    private static final String USER_PHONE = "UserPhone";
    private static final String USER_GENDER = "UserGender";


    private static final String PLACES_LIST = "PlacesList";



    private static final String ACTIVE = "ACTIVE";


    private static final String OTHER_LANGUAGE = "OtherLanguage";



    private static final String CURRENT_LOCATION = "CurrentLocation";


//    private static final String TICKETS_LIST = "TicketsList";

    public PrefManager(Context context) {
        if (context != null){
            this._context = context;
            pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
            editor = pref.edit();
        }
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCHED, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCHED, true);
    }


    public void setActive(boolean active) {
        editor.putBoolean(ACTIVE, active);
        editor.commit();
    }

    public boolean isActive() {
        return pref.getBoolean(ACTIVE, true);
    }

    public void setOtherLanguage(boolean otherLanguage) {
        editor.putBoolean(OTHER_LANGUAGE, otherLanguage);
        editor.commit();
    }

    public boolean usingOtherLanguage() {
        return pref.getBoolean(OTHER_LANGUAGE, false);
    }


    public void setCurrentLocation(String Location) {
        editor.putString(CURRENT_LOCATION, Location);
        editor.apply();
    }

    public String getCurrentLocation() {
        return pref.getString(CURRENT_LOCATION, "No data");
    }


    public String getRequestId() {
        return pref.getString(REQUEST_ID, "No data");
    }

    public void setRequestStatus(String status) {
        editor.putString(REQUEST_STATUS, status);
        editor.apply();
    }

    public String getRequestStatus() {
        return pref.getString(REQUEST_STATUS, "No data");
    }

    public void setIsLoggedIn(boolean IsLoggedIn) {
        editor.putBoolean(IS_LOGGED_IN, IsLoggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGGED_IN, false);
    }

    public void setDoingRequest(boolean DoingRequest) {
        editor.putBoolean(DOING_REQUEST, DoingRequest);
        editor.apply();
    }

    public boolean isDoingRequest() {
        return pref.getBoolean(DOING_REQUEST, false);
    }

    public void setUser(User user){
        editor.putString(USER_FULLNAME, user.getFullName());
        editor.putString(USER_PASSWORD, user.getPassword());
        editor.putString(USER_EMAIL, user.getEmail());
        editor.putString(USER_PHONE, user.getPhone());
        editor.putString(USER_GENDER, user.getGender());
        editor.apply();
    }
    public void setDriver(driver driver){
        editor.putString(USER_FULLNAME, driver.getUsername());
        editor.putString(USER_PASSWORD, driver.getPassword());
        editor.putString(USER_EMAIL, driver.getEmail());
        editor.putString(USER_PHONE, driver.getPhone());
        editor.putString(USER_GENDER, driver.getGender());
        editor.apply();
    }

    public User getUser() {
        return new User(pref.getString(USER_EMAIL, "No data"),
                pref.getString(USER_FULLNAME, "No data"),
                pref.getString(USER_GENDER, "No data"),
                pref.getString(USER_PASSWORD, "No data"),
                pref.getString(USER_PHONE, "No data")
        );
    }

    public driver getDriver() {
        return new driver(
                pref.getString(USER_FULLNAME, "No data"),
                pref.getString(USER_EMAIL, "No data"),
                pref.getString(USER_GENDER, "No data"),
                pref.getString(USER_PASSWORD, "No data"),
                pref.getString(USER_PHONE, "No data")
        );
    }
    public void setPlacesList(String placesList){
        editor.putString(PLACES_LIST, placesList);
        editor.apply();
    }

    public void setRequest(request request){
        editor.putString(REQUEST_STATUS, request.getStatus());
        editor.putString(REQUEST_TIME, request.getTime());
        editor.putString(REQUEST_PRICE, request.getPrice());
        editor.putString(REQUEST_NOTES, request.getNotes());
        editor.putString(REQUEST_ID, request.getRequest_id());
        editor.putString(REQUEST_DEST_STRING,request.getDestString());
        editor.putString(REQUEST_DEST_TEXT,request.getDestText());
        editor.putString(REQUEST_PICKUP_STRING,request.getPickupString());
        editor.putString(REQUEST_PICKUP_TEXT,request.getPickupText());
        editor.putString(REQUEST_PASSENGER_NAME, request.getPassenger_name());
        editor.putString(REQUEST_PASSENGER_PHONE, request.getPassenger_phone());
        editor.apply();
    }

    public request getRequest() {
        return new request(pref.getString(REQUEST_ID,"No data"),
                pref.getString(REQUEST_PICKUP_STRING,"No data"),
                pref.getString(REQUEST_DEST_STRING,"No data"),
                pref.getString(REQUEST_PASSENGER_NAME,"No data"),
                pref.getString(REQUEST_PASSENGER_PHONE,"No data"),
                pref.getString(REQUEST_TIME,"No data"),
                pref.getString(REQUEST_PRICE,"No data"),
                pref.getString(REQUEST_NOTES,"No data"),
                pref.getString(REQUEST_STATUS,"No data"),
                pref.getString(REQUEST_PICKUP_TEXT,"No data"),
                pref.getString(REQUEST_DEST_TEXT,"No data")
        );
    }
//    public String getPlacesList(){
//        ArrayList<MapPlace> placesList = new ArrayList<>();
//        Gson gson = new Gson();
//        String json = gson.toJson(placesList);
//        return pref.getString(PLACES_LIST,json);
//    }

//    public void setTicketsList(String ticketsList){
//        editor.putString(TICKETS_LIST, ticketsList);
//        editor.apply();
//    }

//    public String getTicketsList(){
//        ArrayList<EventTicket> eventsList = new ArrayList<>();
//        Gson gson = new Gson();
//        String json = gson.toJson(eventsList);
//        return pref.getString(TICKETS_LIST,json);
//    }



}