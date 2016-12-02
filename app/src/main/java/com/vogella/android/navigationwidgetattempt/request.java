package com.vogella.android.navigationwidgetattempt;

import android.content.Context;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import static android.content.ContentValues.TAG;

/**
 * Created by nezuma on 10/22/16.
 */

public class request {
    private transient double pickup[] = new double[2];
    private transient double dest[] = new double[2];
    @SerializedName(value = "request_id")
    private String request_id;
    @SerializedName(value = "pickup")
    private String pickupString;
    @SerializedName(value = "pickup_text")
    private String pickupText;
    @SerializedName(value = "dest")
    private String destString;
    @SerializedName(value = "dest_text")
    private String destText;
    @SerializedName(value = "time")
    private String time;
    @SerializedName(value = "price")
    private String price;
    @SerializedName(value = "status")
    private String status;
    @SerializedName(value = "notes")
    private String notes;
    @SerializedName(value = "passenger_name")
    private String passenger_name;
    @SerializedName(value = "passenger_phone")
    private String passenger_phone;

    public request(){
        this.dest = new double[2];
        this.destString = "";
        this.destText = "";
        this.passenger_name = "";
        this.pickup = new double[2];
        this.pickupString = "";
        this.pickupText = "";
        this.time = "";
        this.price = "";
        this.request_id = "";
        this.status = "";
        this.notes = "";
        this.passenger_phone = "";
    }
    public request(String request_id, String pickup, String dest, String passenger_name,
                   String passenger_phone, String time, String price, String notes, String status,
                   String pickupText, String destText){
        this.pickup[0] = Double.parseDouble(pickup.split(",")[0]);
        this.pickup[1] = Double.parseDouble(pickup.split(",")[1]);
        this.dest[0] = Double.parseDouble(dest.split(",")[0]);
        this.dest[1] = Double.parseDouble(dest.split(",")[1]);
        this.passenger_name = passenger_name;
        this.time = time;
        this.price = price;
        this.request_id = request_id;
        this.status = status;
        this.notes = notes;
        this.passenger_phone = passenger_phone;
        this.pickupString = pickup;
        this.destString = dest;
        this.pickupText = pickupText;
        this.destText = destText;
    }

    public request(String request_id, double pickup[], double dest[], String passenger_name,
                   String passenger_phone, String time, String price, String notes, String status,
                   String pickupText, String destText){
        this.pickup = pickup;
        this.dest = dest;
        this.pickupString = String.valueOf(pickup[0]) + "," + String.valueOf(pickup[1]);
        this.destString = String.valueOf(dest[0]) + "," + String.valueOf(dest[1]);
        this.passenger_name = passenger_name;
        this.time = time;
        this.price = price;
        this.request_id = request_id;
        this.status = status;
        this.notes = notes;
        this.passenger_phone = passenger_phone;
        this.pickupText = pickupText;
        this.destText = destText;
    }

    public String getPickupText() {
        return pickupText;
    }

    public void setPickupText(String pickupText) {
        this.pickupText = pickupText;
    }

    public String getDestText() {
        return destText;
    }

    public void setDestText(String destText) {
        this.destText = destText;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getPickupString() {
        return pickupString;
    }

    public void setPickupString(String pickupString) {
        this.pickupString = pickupString;
        this.pickup[0] = Double.parseDouble(pickupString.split(",")[0]);
        this.pickup[1] = Double.parseDouble(pickupString.split(",")[1]);
    }

    public double[] getPickup() {
        return pickup;
    }

    public void setPickup(double[] pickup) {
        this.pickup = pickup;
        this.pickupString = String.valueOf(pickup[0]) + "," + String.valueOf(pickup[1]);
    }

    public String getDestString() {
        return destString;
    }

    public void setDestString(String destString) {
        this.destString = destString;
        this.dest[0] = Double.parseDouble(destString.split(",")[0]);
        this.dest[1] = Double.parseDouble(destString.split(",")[1]);

    }

    public double[] getDest() {
        return dest;
    }

    public void setDest(double[] dest) {
        this.dest = dest;
        this.destString = String.valueOf(dest[0]) + "," + String.valueOf(dest[1]);

    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPassenger_name() {
        return passenger_name;
    }

    public void setPassenger_name(String passenger_name) {
        this.passenger_name = passenger_name;
    }

    public String getPassenger_phone() {
        return passenger_phone;
    }

    public void setPassenger_phone(String passenger_phone) {
        this.passenger_phone = passenger_phone;
    }

    public void nextStatus() {
        switch (this.status) {
            case "on_the_way":
                this.status = "arrived_pickup";
                break;
            case "arrived_pickup":
                this.status = "passenger_onboard";
                break;
            case "passenger_onboard":
                this.status = "arrived_dest";
                break;
            case "arrived_dest":
                this.status = "completed";
                break;
        }
    }


    public String getNextStatus(){
        switch (this.status) {
            case "on_the_way":
                return "arrived_pickup";
            case "arrived_pickup":
                return "passenger_onboard";
            case "passenger_onboard":
                return "arrived_dest";
            case "arrived_dest":
                return "completed";
            default:
                return "error";
        }
    }
    public String getDisplayStatus(String status, Context context){
        switch (status) {
            case "accepted":
                return context.getString(R.string.accepted);
            case "canceled":
                return context.getString(R.string.canceled);
            case "on_the_way":
                return context.getString(R.string.on_the_way);
            case "arrived_pickup":
                return context.getString(R.string.arrived_pickup);
            case "passenger_onboard":
                return context.getString(R.string.passenger_onboard);
            case "arrived_dest":
                return context.getString(R.string.arrived_dest);
            case "completed":
                return context.getString(R.string.completed);
            default:
                return status;
        }
    }
}
