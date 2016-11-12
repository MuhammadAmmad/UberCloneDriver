package com.vogella.android.navigationwidgetattempt;

/**
 * Created by nezuma on 10/22/16.
 */

public class driver {
    String username;
    String email;
    String phone;
    String gender;

    public driver(String username, String email, String gender, String phone){
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.username = username;
    }
    public driver(){
        this.email = null;
        this.username = null;
        this.phone = null;
        this.gender = null;
    }
}
