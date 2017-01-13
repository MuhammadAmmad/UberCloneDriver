package com.Wisam.passenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by nezuma on 10/26/16.
 */

public class RequestAdapter extends RecyclerView.Adapter <RequestAdapter.RequestViewHolder> {

    private static final int FROM_HISTORY_CODE = 899;
    private List<request> RequestList;
    private Context context;

    public RequestAdapter(List<request> RequestList, Context context) {
        this.RequestList = RequestList;
        this.context = context;
    }


    @Override
    public int getItemCount() {
        return RequestList.size();
    }

    @Override
    public void onBindViewHolder(RequestViewHolder RequestViewHolder, int i) {
        final request ci = RequestList.get(i);
        RequestViewHolder.price.setText(ci.getPrice());
        RequestViewHolder.date.setText(ci.getTime());
        switch (ci.getStatus()){
            case "completed":
                RequestViewHolder.status.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
                break;
            case "canceled":
                RequestViewHolder.status.setBackgroundColor(context.getResources().getColor(R.color.red));
                break;
            case "missed":
                RequestViewHolder.status.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
                break;

        }
        RequestViewHolder.pickup.setText(ci.getPickupText().replaceAll("\n", " "));
        RequestViewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SelectedRequest.class);
                intent.putExtra("passenger_name", ci.getPassenger_name());
                intent.putExtra("passenger_phone", ci.getPassenger_phone());
                intent.putExtra("status", ci.getStatus());
                intent.putExtra("pickup", ci.getPickupString());
                intent.putExtra("dest", ci.getDestString());
                intent.putExtra("time", ci.getTime());
                intent.putExtra("price", ci.getPrice());
                intent.putExtra("notes", ci.getNotes());
                intent.putExtra("request_id", ci.getRequest_id());
                intent.putExtra("pickup_text", ci.getPickupText());
                intent.putExtra("dest_text", ci.getDestText());

                intent.putExtra("source", "history");

                ((Activity) context).startActivityForResult(intent, FROM_HISTORY_CODE);
            }
        });
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.history_entry_layout, viewGroup, false);

        return new RequestViewHolder(itemView);
    }

    public void updateRequestsList(List<request> history) {this.RequestList = history;}

    public static class RequestViewHolder extends RecyclerView.ViewHolder{
        protected TextView price;
        protected TextView date;
        protected ImageView status;
        protected TextView pickup;
        protected LinearLayout linearLayout;


        public RequestViewHolder(View v){
            super(v);

            date = (TextView) v.findViewById(R.id.entry_date);
            status = (ImageView) v.findViewById(R.id.entry_status);
            pickup = (TextView) v.findViewById(R.id.entry_from);
            price = (TextView) v.findViewById(R.id.entry_price);
            linearLayout = (LinearLayout) itemView.findViewById(R.id.history_card_view);
        }
    }

}