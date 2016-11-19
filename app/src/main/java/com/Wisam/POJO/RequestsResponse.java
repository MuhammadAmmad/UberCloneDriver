package com.Wisam.POJO;

import com.google.gson.annotations.SerializedName;
import com.vogella.android.navigationwidgetattempt.request;

import java.util.List;

/**
 * Created by nezuma on 11/19/16.
 */

public class RequestsResponse {
    @SerializedName(value = "status")
    private int status;

    @SerializedName(value = "rides")
    public List<request> rides;

    public List<request> getRides() {
        return rides;
    }

    public int getStatus() {
        return status;
    }
}
