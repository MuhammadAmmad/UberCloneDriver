package com.Wisam.POJO;

import com.google.gson.annotations.SerializedName;

/**
 * Created by nezuma on 11/21/16.
 */

public class StatusResponse {
    @SerializedName(value = "status")
    private int status;
    public int getStatus() {
        return status;
    }
}
