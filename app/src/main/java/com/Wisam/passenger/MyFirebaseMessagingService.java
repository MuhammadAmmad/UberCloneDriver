/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.Wisam.passenger;

        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.content.Context;
        import android.content.Intent;
        import android.support.v4.app.NotificationCompat;
        import android.util.Base64;
        import android.util.Log;

        import com.Wisam.Events.DriverLoggedout;
        import com.Wisam.Events.PassengerArrived;
        import com.Wisam.Events.PassengerCanceled;
        import com.Wisam.Events.UnbindBackgroundLocationService;
        import com.Wisam.POJO.StatusResponse;
        import com.google.firebase.messaging.FirebaseMessagingService;
        import com.google.firebase.messaging.RemoteMessage;

        import org.greenrobot.eventbus.EventBus;

        import java.util.Map;

        import retrofit2.Call;
        import retrofit2.Callback;
        import retrofit2.Response;
        import retrofit2.Retrofit;
        import retrofit2.converter.gson.GsonConverterFactory;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static request received = new request();
    private PrefManager prefManager;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        //data includes: request_id, price, pickup, time
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        prefManager = new PrefManager(this);

        // Check if message contains a data payload.
        String pickup = "you didn't tell me where !!";
        String dest = "you didn't tell me where !!";
        String status = "-1";
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> request = remoteMessage.getData();
            for (Map.Entry<String, String> field : request.entrySet()) {
                Log.d(TAG, field.getKey() + "  :  " + field.getValue());
                if (field.getKey().equals("request_id"))
                    received.setRequest_id(field.getValue());
                if (field.getKey().equals("price"))
                    received.setPrice(field.getValue());
                if (field.getKey().equals("time")) {
                    Log.d(TAG, "time received from server =" + field.getValue());
                    received.setTime(field.getValue());
                }
                if (field.getKey().equals("notes"))
                    received.setNotes(field.getValue());
                if (field.getKey().equals("passenger_phone"))
                    received.setPassenger_phone(field.getValue());
                if (field.getKey().equals("passenger_name"))
                    received.setPassenger_name(field.getValue());
                if (field.getKey().equals("dest_text"))
                    received.setDestText(field.getValue());
                if (field.getKey().equals("pickup_text"))
                    received.setPickupText(field.getValue());
                if (field.getKey().equals("pickup")) {
                    pickup = field.getValue();
                }
                if (field.getKey().equals("dest")) {
                    dest = field.getValue();
                }
                if (field.getKey().equals("status")) {
                    status = field.getValue();
                }
            }
            if(prefManager.isLoggedIn()){
                if (status.equals("-1")) {
                    Log.d(TAG, "This request has no status.. it is impossible to determine which API is this");
                } else if (status.equals("0")) {
                    if (prefManager.isActive()) {
                        Intent intent = new Intent(this, FCMRequest.class);
                        intent.putExtra("request_id", received.getRequest_id());
                        intent.putExtra("price", received.getPrice());
                        intent.putExtra("pickup", pickup);
                        intent.putExtra("pickup_text", received.getPickupText());
                        Log.d(TAG, "time read from the first request object =" + received.getTime());
                        intent.putExtra("time", received.getTime());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("dest", dest);
                        intent.putExtra("dest_text", received.getDestText());
                        intent.putExtra("passenger_name", received.getPassenger_name());
                        intent.putExtra("passenger_phone", received.getPassenger_phone());
                        intent.putExtra("notes", received.getNotes());
                        startActivity(intent);
                    } else {
                        serverAccept(received.getRequest_id(), 0);
                    }
                    Log.d(TAG, "The recieved request has the content:" + received);
                } else if (status.equals("1")) { // Passenger Canceled
                    for (Map.Entry<String, String> field : request.entrySet()) {
                        if (field.getKey().equals("request_id")) {
                            prefManager.setFcmrequestStatus("canceled");
                            prefManager.setFcmrequestId(field.getValue());
                            EventBus.getDefault().post(new PassengerCanceled(field.getValue()));
                            sendNotification(getString(R.string.passenger_cancelled_notification) + field.getValue());
                            break;
                        }
                    }
                } else if (status.equals("2")) { // Passenger Arrived
                    for (Map.Entry<String, String> field : request.entrySet()) {
                        if (field.getKey().equals("request_id")) {
                            sendNotification(getString(R.string.passenger_completed_notification));
                            prefManager.setFcmrequestStatus("completed");
                            prefManager.setFcmrequestId(field.getValue());
                            EventBus.getDefault().post(new PassengerArrived(field.getValue()));
                        }
                    }
                } else if (status.equals("3")) { // Driver logged out
                    sendNotification(getString(R.string.logged_elsewhere));

                    logout();
                }else if(status.equals("10")){//Update BASEURL
                    String baseURL = request.get("base_url");
                    if(baseURL != null) {
                        PrefManager prefManager;
                        prefManager = new PrefManager(MyFirebaseMessagingService.this);
                        prefManager.setBaseUrl(baseURL);
                    }
                }
            }
        }
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }


    private void serverAccept(String request_id, final int accepted ) {
        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(MyFirebaseMessagingService.this))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail","");
        String password = prefManager.pref.getString("UserPassword","");

        Log.d(TAG, "serverAccept");



        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.accept("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP),
                request_id,accepted);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null){
                    Log.d(TAG, "You have rejected the request");
                } else if (response.code() == 401){
                    Log.i(TAG, "onResponse: User not logged in");
                    logout();

                } else {
//                    clearHistoryEntries();
                    Log.d(TAG,getResources().getString(R.string.server_unknown_error));
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                Log.d(TAG, "The response is onFailure");
            }
        });
    }

    private void logout() {
        String lastEmail = prefManager.getLastEmail();
        String lastPassword = prefManager.getLastPassword();
        prefManager.editor.clear().apply();
        prefManager.setLastPassword(lastPassword);
        prefManager.setLastEmail(lastEmail);
        prefManager.setIsLoggedIn(false);
//                    prefManager.setExternalLogout(false);
        EventBus.getDefault().post(new UnbindBackgroundLocationService());
        Intent blsIntent = new Intent(getApplicationContext(), BackgroundLocationService.class);
        stopService(blsIntent);

        EventBus.getDefault().post(new DriverLoggedout());
    }

}