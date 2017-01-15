package com.Wisam.passenger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.DriverLoggedout;
import com.Wisam.Events.UnbindBackgroundLocationService;
import com.Wisam.POJO.StatusResponse;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by nezuma on 10/27/16.
 */

public class OngoingRequestAdapter extends RecyclerView.Adapter <com.Wisam.passenger.OngoingRequestAdapter.OngoingRequestViewHolder> {

    private static final String TAG = "UbDriver";
    private static final int SELECTED_REQUEST_CODE = 43542;
    private List<request> requestList;
    private Activity context;
    private PrefManager prefManager;

    public OngoingRequestAdapter(List<request> RequestList, Activity context) {
        this.requestList = RequestList;
        this.context = context;
    }


    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public boolean addRequest(request request){
        requestList.add(getItemCount(), request); return true;}

    @Override
    public void onBindViewHolder(final com.Wisam.passenger.OngoingRequestAdapter.OngoingRequestViewHolder RequestViewHolder, int i) {
        final request ci = requestList.get(i);

        RequestViewHolder.price.setText(ci.getPrice());
        RequestViewHolder.price.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        RequestViewHolder.date.setText(ci.getTime());
        RequestViewHolder.status.setBackgroundColor(context.getResources().getColor(R.color.white));
        RequestViewHolder.pickup.setText(ci.getPickupText());

        prefManager = new PrefManager(context);

        RequestViewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SelectedRequest.class);
                intent.putExtra("passenger_name", ci.getPassenger_name());
                intent.putExtra("passenger_phone", ci.getPassenger_phone());
                intent.putExtra("status", "on_the_way");
                intent.putExtra("pickup", ci.getPickupString());
                intent.putExtra("dest", ci.getDestString());
                intent.putExtra("time", ci.getTime());
                intent.putExtra("price", ci.getPrice());
                intent.putExtra("notes", ci.getNotes());
                intent.putExtra("request_id", ci.getRequest_id());
                intent.putExtra("pickup_text", ci.getPickupText());
                intent.putExtra("dest_text", ci.getDestText());

                intent.putExtra("source", "incoming");

                ((Activity) context).startActivityForResult(intent,SELECTED_REQUEST_CODE);
             }
        });
    }

    @Override
    public com.Wisam.passenger.OngoingRequestAdapter.OngoingRequestViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.history_entry_layout, viewGroup, false);

        return new com.Wisam.passenger.OngoingRequestAdapter.OngoingRequestViewHolder(itemView);
    }

    public void updateRequestsList(List<request> requestList) {
        this.requestList = requestList;
    }

    public static class OngoingRequestViewHolder extends RecyclerView.ViewHolder{

        protected TextView price;
        protected TextView date;
        protected ImageView status;
        protected TextView pickup;
        protected LinearLayout linearLayout;

        public OngoingRequestViewHolder(View v){
            super(v);
            date = (TextView) v.findViewById(R.id.entry_date);
            status = (ImageView) v.findViewById(R.id.entry_status);
            pickup = (TextView) v.findViewById(R.id.entry_from);
            price = (TextView) v.findViewById(R.id.entry_price);
            linearLayout = (LinearLayout) itemView.findViewById(R.id.history_card_view);
        }
    }

    private void sendCancel(final String request_id) {
        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        final ProgressDialog progress = new ProgressDialog(context);
        progress.setMessage(context.getString(R.string.cancelling_request));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();


        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.cancel("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                request_id);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if(!context.isFinishing() && progress != null && progress.isShowing())progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
                    removeRequest(request_id);
                    Toast.makeText(context, R.string.request_cancelled_successfully, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The request has been cancelled successfully");
                } else if (response.code() == 401) {
                    Toast.makeText(context, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onResume: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    String lastEmail = prefManager.getLastEmail();
                    String lastPassword = prefManager.getLastPassword();
                    prefManager.editor.clear().apply();
                    prefManager.setLastPassword(lastPassword);
                    prefManager.setLastEmail(lastEmail);
                    prefManager.setIsLoggedIn(false);
                    EventBus.getDefault().post(new UnbindBackgroundLocationService());
                    Intent blsIntent = new Intent(context.getApplicationContext(), BackgroundLocationService.class);
                    context.stopService(blsIntent);

                    EventBus.getDefault().post(new DriverLoggedout());
                } else {
                    Toast.makeText(context, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if(!context.isFinishing() && progress != null && progress.isShowing())progress.dismiss();
                Toast.makeText(context, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Couldn't connect to the server");
            }
        });
    }

    private void removeRequest(String request_id) {
        int i;
        for (i = 0; i < this.requestList.size(); i++) {
            if (requestList.get(i).getRequest_id().equals(request_id)) {
                requestList.remove(requestList.get(i));
                Log.d(TAG, "The request " + request_id + " has been removed");
                this.notifyDataSetChanged();
                break;
            }
        }
    }
}

