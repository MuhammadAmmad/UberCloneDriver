package com.Wisam.passenger;

import com.google.gson.annotations.SerializedName;

/**
 * Created by nezuma on 10/22/16.
 */

public class driver {
    @SerializedName(value = "fullname")
    String username;
    @SerializedName(value = "email")
    String email;
    @SerializedName(value = "phone")
    String phone;
    @SerializedName(value = "gender")
    String gender;
    @SerializedName(value = "password")
    String password;

    public driver(String username, String email, String gender, String phone, String password){
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.password = password;
    }
    public driver(){
        this.email = null;
        this.username = null;
        this.phone = null;
        this.gender = null;
        this.password = null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
