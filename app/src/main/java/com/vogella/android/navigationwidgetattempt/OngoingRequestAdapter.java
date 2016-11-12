package com.vogella.android.navigationwidgetattempt;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by nezuma on 10/27/16.
 */

public class OngoingRequestAdapter extends RecyclerView.Adapter <com.vogella.android.navigationwidgetattempt.OngoingRequestAdapter.OngoingRequestViewHolder> {

    private List<request> RequestList;
    private Context context;

    public OngoingRequestAdapter(List<request> RequestList, Context context) {
        this.RequestList = RequestList;
        this.context = context;
    }


    @Override
    public int getItemCount() {
        return RequestList.size();
    }

    @Override
    public void onBindViewHolder(final com.vogella.android.navigationwidgetattempt.OngoingRequestAdapter.OngoingRequestViewHolder RequestViewHolder, int i) {
        final request ci = RequestList.get(i);

        RequestViewHolder.price.setText(ci.price);
        RequestViewHolder.date.setText(ci.time);
        RequestViewHolder.status.setText(ci.status);
        RequestViewHolder.pickup.setText(String.valueOf(ci.pickup[0]) + "," + String.valueOf(ci.pickup[1]));
        RequestViewHolder.dest.setText(String.valueOf(ci.dest[0]) + "," + String.valueOf(ci.dest[1]));

/*
        RequestViewHolder.passenger_name.setText(ci.passenger_name);
        RequestViewHolder.passenger_phone.setText(ci.passenger_phone);
        RequestViewHolder.status.setText(ci.status);
        RequestViewHolder.pickup.setText(String.valueOf(ci.pickup[0]) + "," + String.valueOf(ci.pickup[1]));
        RequestViewHolder.dest.setText(String.valueOf(ci.dest[0]) + "," + String.valueOf(ci.dest[1]));
        RequestViewHolder.request_time.setText(ci.time);
*/

        RequestViewHolder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alerBuilder = new AlertDialog.Builder(context);
                alerBuilder.setMessage("Do you want to start this request?");
                alerBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.putExtra("passenger_name", ci.passenger_name);
                        intent.putExtra("passenger_phone", ci.passenger_phone);
                        intent.putExtra("status", ci.status);
//                        String temp[] = RequestViewHolder.pickup.getText().toString().split(",");
                        intent.putExtra("pickup_longitude", ci.pickup[0]);
                        intent.putExtra("pickup_latitude", ci.pickup[1]);
//                        temp = RequestViewHolder.dest.getText().toString().split(",");
                        intent.putExtra("dest_longitude", ci.dest[0]);
                        intent.putExtra("dest_latitude", ci.dest[1]);
                        intent.putExtra("time", ci.time);
                        intent.putExtra("price", ci.price);
                        intent.putExtra("notes", ci.notes);
                        intent.putExtra("request_id", ci.request_id);

                        ((Activity)context).setResult(Activity.RESULT_OK, intent);
                        ((Activity)context).finish();

                    }
                });
                alerBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alerBuilder.show();

            }
        });
    }

    @Override
    public com.vogella.android.navigationwidgetattempt.OngoingRequestAdapter.OngoingRequestViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.history_entry_layout, viewGroup, false);

        return new com.vogella.android.navigationwidgetattempt.OngoingRequestAdapter.OngoingRequestViewHolder(itemView);
    }

    public static class OngoingRequestViewHolder extends RecyclerView.ViewHolder{

        protected TextView price;
        protected TextView date;
        protected TextView status;
        protected TextView pickup;
        protected TextView dest;
        protected CardView card;

/*
        protected TextView passenger_name;
        protected TextView passenger_phone;
        protected TextView request_time;
        protected TextView status;
        protected TextView pickup;
        protected TextView dest;
        RelativeLayout card;
*/

        public OngoingRequestViewHolder(View v){
            super(v);
            date = (TextView) v.findViewById(R.id.entry_date);
            status = (TextView) v.findViewById(R.id.entry_status);
            pickup = (TextView) v.findViewById(R.id.entry_from);
            dest = (TextView) v.findViewById(R.id.entry_to);
            price = (TextView) v.findViewById(R.id.entry_price);
            card = (CardView) itemView.findViewById(R.id.history_card_view);


/*
            passenger_name = (TextView) v.findViewById(R.id.card_passenger_name);
            passenger_phone = (TextView) v.findViewById(R.id.card_passenger_phone);
            request_time = (TextView) v.findViewById(R.id.card_time);
            status = (TextView) v.findViewById(R.id.card_status);
            pickup = (TextView) v.findViewById(R.id.card_pickup);
            dest = (TextView) v.findViewById(R.id.card_dest);
            card = (RelativeLayout) itemView.findViewById(R.id.card_view);
*/
        }
    }

}

