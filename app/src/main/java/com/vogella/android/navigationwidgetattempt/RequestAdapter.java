package com.vogella.android.navigationwidgetattempt;

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
//        RequestViewHolder.status.setText(ci.getDisplayStatus(ci.getStatus(), context));
        switch (ci.getStatus()){
            case "completed":
                RequestViewHolder.status.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
                break;
            case "canceled":
                RequestViewHolder.status.setBackgroundColor(context.getResources().getColor(R.color.red));
                break;
            case "missed":
//                RequestViewHolder.status.setBackground(context.getResources().getColor(R.color.colorAccent));
                RequestViewHolder.status.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
                break;

        }
        RequestViewHolder.pickup.setText(ci.getPickupText().replaceAll("\n", " "));
//        RequestViewHolder.dest.setText(ci.getDestText());
//        RequestViewHolder.pickup.setText(String.valueOf(ci.getPickup()[0]) + "," + String.valueOf(ci.getPickup()[1]));
//        RequestViewHolder.dest.setText(String.valueOf(ci.getDest()[0]) + "," + String.valueOf(ci.getDest()[1]));
//        RequestViewHolder.request_time.setText(ci.time);

        RequestViewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SelectedRequest.class);
                intent.putExtra("passenger_name", ci.getPassenger_name());
                intent.putExtra("passenger_phone", ci.getPassenger_phone());
                intent.putExtra("status", ci.getStatus());
//                        String temp[] = RequestViewHolder.pickup.getText().toString().split(",");
//                intent.putExtra("pickup_latitude", ci.getPickup()[0]);
//                intent.putExtra("pickup_longitude", ci.getPickup()[1]);
                intent.putExtra("pickup", ci.getPickupString());
//                        temp = RequestViewHolder.dest.getText().toString().split(",");
//                intent.putExtra("dest_latitude", ci.getDest()[0]);
//                intent.putExtra("dest_longitude", ci.getDest()[1]);
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
//        protected TextView status;
        protected ImageView status;
        protected TextView pickup;
//        protected TextView dest;
        protected LinearLayout linearLayout;


        public RequestViewHolder(View v){
            super(v);

            date = (TextView) v.findViewById(R.id.entry_date);
//            status = (TextView) v.findViewById(R.id.entry_status);
            status = (ImageView) v.findViewById(R.id.entry_status);
            pickup = (TextView) v.findViewById(R.id.entry_from);
//            dest = (TextView) v.findViewById(R.id.entry_to);
            price = (TextView) v.findViewById(R.id.entry_price);
            linearLayout = (LinearLayout) itemView.findViewById(R.id.history_card_view);

        }
    }

}
