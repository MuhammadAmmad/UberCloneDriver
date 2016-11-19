package com.vogella.android.navigationwidgetattempt;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import static android.content.ContentValues.TAG;

/**
 * Created by nezuma on 10/22/16.
 */

public class request {
    @SerializedName(value = "request_id")
    public String request_id;
    @SerializedName(value = "pickup")
    public double pickup[] = new double[2];
    @SerializedName(value = "dest")
    public double dest[] = new double[2];
    @SerializedName(value = "time")
    public String time;
    @SerializedName(value = "price")
    public String price;
    @SerializedName(value = "status")
    public String status;
    @SerializedName(value = "notes")
    public String notes;
    @SerializedName(value = "passenger_name")
    public String passenger_name;
    @SerializedName(value = "passenger_phone")
    public String passenger_phone;

    public request(){
        this.dest = new double[2];
        this.passenger_name = "";
        this.pickup = new double[2];
        this.time = "";
        this.price = "";
        this.request_id = "";
        this.status = "";
        this.notes = "";
        this.passenger_phone = "";
    }
    public request(String request_id, double pickup[], double dest[], String passenger_name,
                   String passenger_phone, String time, String price, String notes, String status){

        try {
            this.dest[0] = dest[0];
            this.dest[1] = dest[1];
        }
        catch (NullPointerException e){
            Log.d(TAG, "request: dest[0] = " + String.valueOf(dest[0]));
            Log.d(TAG, "request: dest[1] = " + String.valueOf(dest[1]));
            Log.d(TAG, "request: I guess the problem lies with this.dest? ");
        }


        try {
            this.pickup[0] = pickup[0];
            this.pickup[1] = pickup[1];
        }
        catch (NullPointerException e){
            Log.d(TAG, "request: pickup[0] = " + String.valueOf(pickup[0]));
            Log.d(TAG, "request: pickup[1] = " + String.valueOf(pickup[1]));
            Log.d(TAG, "request: I guess the problem lies with this.pickup? ");
        }

        this.passenger_name = passenger_name;
        this.time = time;
        this.price = price;
        this.request_id = request_id;
        this.status = status;
        this.notes = notes;
        this.passenger_phone = passenger_phone;
    }

/*    public void setDest(String dest) {
        this.dest = dest;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPassenger_name(String passenger_name) {
        this.passenger_name = passenger_name;
    }

    public void setPassenger_phone(String passenger_phone) {
        this.passenger_phone = passenger_phone;
    }

    public void setPickup(String pickup) {
        this.pickup = pickup;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTime(String time) {
        this.time = time;
    }*/

    public void nextStatus() {
        switch (this.status) {
            case "on the way":
                this.status = "arrived at pickup point";
                break;
            case "arrived at pickup point":
                this.status = "passenger picked up";
                break;
            case "passenger picked up":
                this.status = "arrived at destination";
                break;
            case "arrived at destination":
                this.status = "completed";
                break;
        }
    }


    public String getNextStatus(){
        switch (this.status) {
            case "on the way":
                return "arrived at pickup point";
            case "arrived at pickup point":
                return "passenger picked up";
            case "passenger picked up":
                return "arrived at destination";
            case "arrived at destination":
                return "completed";
            default:
                return "error";
        }
    }
}
