package com.Wisam.POJO;

import com.google.gson.annotations.SerializedName;
import com.vogella.android.navigationwidgetattempt.driver;

/**
 * Created by nezuma on 11/19/16.
 */

public class LoginResponse {
    @SerializedName(value = "status")
    private int status;

    @SerializedName(value = "user")
    private driver driver;

    @SerializedName(value = "error_msg")
    private String errorMessage;

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getStatus() {
        return status;
    }

    public com.vogella.android.navigationwidgetattempt.driver getdriver() {
        return driver;
    }
}