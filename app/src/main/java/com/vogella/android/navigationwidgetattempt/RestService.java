package com.vogella.android.navigationwidgetattempt;

import com.Wisam.POJO.LoginResponse;
import com.Wisam.POJO.RequestsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Created by nezuma on 11/19/16.
 */
public interface RestService {
//    @GET("passenger_api/login")
//    Call<LoginResponse> login(@Header("Authorization") String authorization );
    @GET("driver_api/login")
    Call<LoginResponse> login(@Header("Authorization") String authorization );

    @GET("driver_api/requests")
    Call<RequestsResponse> requests(@Header("Authorization") String authorization );

    @GET("driver_api/history")
    Call<RequestsResponse> history(@Header("Authorization") String authorization );

//    @GET("passenger_api/register")
//    Call<SimpleResponse> register(
//            @Query("email") String email,
//            @Query("fullname") String fullname,
//            @Query("password") String password,
//            @Query("phone") String phone,
//            @Query("gender") String gender
//    );
//
//    @GET("passenger_api/get_drivers")
//    Call<DriversResponse> getDrivers(@Query("location") String location);
//
//
//    @GET("passenger_api/get_drivers")
//    Call<DriverResponse> getDriver(
//            @Query("pickup") String pickup,
//            @Query("dest") String dest,
//            @Query("time") String time,
//            @Query("female_driver") Boolean female_driver,
//            @Query("notes") String notes,
//            @Query("price") String price,
//            @Query("request_id") String request_id
//
//    );
}
