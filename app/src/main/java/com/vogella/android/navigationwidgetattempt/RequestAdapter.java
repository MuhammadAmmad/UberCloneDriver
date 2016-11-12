package com.vogella.android.navigationwidgetattempt;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by nezuma on 10/26/16.
 */

public class RequestAdapter extends RecyclerView.Adapter <RequestAdapter.RequestViewHolder> {

    private List<request> RequestList;

    public RequestAdapter(List<request> RequestList) {
        this.RequestList = RequestList;
    }


    @Override
    public int getItemCount() {
        return RequestList.size();
    }

    @Override
    public void onBindViewHolder(RequestViewHolder RequestViewHolder, int i) {
        request ci = RequestList.get(i);
        RequestViewHolder.price.setText(ci.price);
        RequestViewHolder.date.setText(ci.time);
        RequestViewHolder.status.setText(ci.status);
        RequestViewHolder.pickup.setText(String.valueOf(ci.pickup[0]) + "," + String.valueOf(ci.pickup[1]));
        RequestViewHolder.dest.setText(String.valueOf(ci.dest[0]) + "," + String.valueOf(ci.dest[1]));
//        RequestViewHolder.request_time.setText(ci.time);
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.history_entry_layout, viewGroup, false);

        return new RequestViewHolder(itemView);
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder{
        protected TextView price;
        protected TextView date;
        protected TextView status;
        protected TextView pickup;
        protected TextView dest;

        public RequestViewHolder(View v){
            super(v);

            date = (TextView) v.findViewById(R.id.entry_date);
            status = (TextView) v.findViewById(R.id.entry_status);
            pickup = (TextView) v.findViewById(R.id.entry_from);
            dest = (TextView) v.findViewById(R.id.entry_to);
            price = (TextView) v.findViewById(R.id.entry_price);
        }
    }

}
