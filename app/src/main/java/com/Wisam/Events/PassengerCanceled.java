package com.Wisam.Events;

/**
 * Created by nezuma on 11/24/16.
 */

public class PassengerCanceled {
    private String request_id;

    public PassengerCanceled(String request_id) {
        this.request_id = request_id;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }
}
