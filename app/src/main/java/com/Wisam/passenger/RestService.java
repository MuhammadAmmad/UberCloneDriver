package com.Wisam.passenger;

import com.Wisam.POJO.StatusResponse;
import com.Wisam.POJO.LoginResponse;
import com.Wisam.POJO.RequestsResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by nezuma on 11/19/16.
 */
public interface RestService {
//    @GET("passenger_api/login")
//    Call<LoginResponse> login(@Header("Authorization") String authorization );
    @FormUrlEncoded
    @POST("driver_api/login/")
    Call<LoginResponse> login(@Header("Authorization") String authorization, @Field("registration_token") String registration_token,@Field("version_code") int version_code);

//    @FormUrlEncoded
    @POST("driver_api/requests/")
    Call<RequestsResponse> requests(@Header("Authorization") String authorization );

//    @FormUrlEncoded
    @POST("driver_api/history/")
    Call<RequestsResponse> history(@Header("Authorization") String authorization );

    @FormUrlEncoded
    @POST("driver_api/accept/")
    Call<StatusResponse> accept(@Header("Authorization") String authorization, @Field("request_id")String request_id, @Field("accepted") int accepted);

    @FormUrlEncoded
    @POST("driver_api/active/")
    Call<StatusResponse> active(@Header("Authorization") String authorization, @Field("active") int active, @Field("location")String location);

    @FormUrlEncoded
    @POST("driver_api/status/")
    Call<StatusResponse> status(@Header("Authorization") String authorization, @Field("request_id")String request_id, @Field("status") String status);

//    @FormUrlEncoded
    @GET("driver_api/cancel/")
    Call<StatusResponse> cancel(@Header("Authorization") String authorization, @Query("request_id") String request_id);

    @FormUrlEncoded
    @POST("driver_api/location/")
    Call<StatusResponse> location(@Header("Authorization") String authorization, @Field("request_id")String request_id, @Field("location")String location);

    @FormUrlEncoded
    @POST("driver_api/token/")
    Call<StatusResponse> token(@Header("Authorization") String authorization, @Field("registration_token")String registration_token);

}
