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

package com.vogella.android.navigationwidgetattempt;

        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.content.Context;
        import android.content.Intent;
        import android.media.RingtoneManager;
        import android.net.Uri;
        import android.support.v4.app.NotificationCompat;
        import android.util.Log;

        import com.Wisam.Events.DriverLoggedout;
        import com.Wisam.Events.PassengerArrived;
        import com.Wisam.Events.PassengerCanceled;
        import com.google.firebase.messaging.FirebaseMessagingService;
        import com.google.firebase.messaging.RemoteMessage;

        import org.greenrobot.eventbus.EventBus;

        import java.util.Map;
        import java.util.Objects;

//current token : dKkBmm8H48A:APA91bFQQR2f-ibM1EfuLXbIRTItS2M3l5oV4AosbyEDZLdWm9un_-CJArBXNHo-lAonoXAqrlEy-tgbik4K3Hd5NJeKjgVjSG0tavW1_swW38oUIHbRN9uwVCPE06ujZh6szCH5glgi

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static request received = new request();
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

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        //data includes: request_id, price, pickup, time
        Log.d(TAG, "From: " + remoteMessage.getFrom());

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
                if (field.getKey().equals("time"))
                    received.setTime(field.getValue());
                if (field.getKey().equals("notes"))
                    received.setNotes(field.getValue());
                if (field.getKey().equals("passenger_phone"))
                    received.setPassenger_phone(field.getValue());
                if (field.getKey().equals("passenger_name"))
                    received.setPassenger_name(field.getValue());
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
            if (status.equals("-1")) {
                Log.d(TAG, "This request has no status.. it is impossible to determine which API is this");
            }
            else if(status.equals("0")){
                Intent intent = new Intent(this, FCMRequest.class);
                intent.putExtra("request_id", received.getRequest_id());
                intent.putExtra("price", received.getPrice());
                intent.putExtra("pickup", pickup);
                intent.putExtra("time", received.getTime());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("dest", dest);
//                intent.putExtra("time", received.getTime());
                intent.putExtra("passenger_name", received.getPassenger_name());
                intent.putExtra("passenger_phone", received.getPassenger_phone());
                intent.putExtra("notes", received.getNotes());
                startActivity(intent);
                Log.d(TAG, "The recieved request has the content:" + received);
            }
            else if (status.equals("1")){ // Passenger Canceled
                for (Map.Entry<String, String> field : request.entrySet()) {
                    if (field.getKey().equals("request_id")) {
                        EventBus.getDefault().post(new PassengerCanceled(field.getValue()));
                        OngoingRequestsActivity.removeRequest(field.getValue());
                        sendNotification("The passenger Canceled the request no." + field.getValue());
                        break;
                    }
                }
            }
            else if (status.equals("2")){ // Passenger Arrived
                for (Map.Entry<String, String> field : request.entrySet()) {
                    if (field.getKey().equals("request_id")) {
                        sendNotification("The request has been completed");
                        EventBus.getDefault().post(new PassengerArrived(field.getValue()));
                        OngoingRequestsActivity.removeRequest(field.getValue());
                       // break;
                    }
                }
            }
            else if (status.equals("3")){ // Driver logged out
                sendNotification("You are logged out of this device." +
                        "Note that you can be loggedin on one device only");
                EventBus.getDefault().post(new DriverLoggedout());
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

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("FCM Message")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}