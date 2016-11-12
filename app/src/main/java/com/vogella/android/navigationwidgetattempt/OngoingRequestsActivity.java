package com.vogella.android.navigationwidgetattempt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class OngoingRequestsActivity extends AppCompatActivity{

    private static final String DUMMY_REQUEST_ID = "1243";
    private static final double DUMMY_PICKUP[] = {15.6023428,32.5873593};
//    private static final String DUMMY_PICKUP = "15.5838046,32.5543825";
//    private static final String DUMMY_DEST = "15.8838046, 32.6543825";
    private static final double DUMMY_DEST[] = {15.5551185, 32.5543017};
    private static final String DUMMY_PASSENGER_NAME = "John Green";
    private static final String DUMMY_PASSENGER_PHONE = "0123456789";
    private static final String DUMMY_STATUS = "on the way";
    private static final String DUMMY_NOTES = "Drive slowly";
    private static final String DUMMY_PRICE = "43";
    private static final String DUMMY_TIME = "06/11/2016 ; 15:45";
    //    private static request current_request = new request(DUMMY_REQUEST_ID, DUMMY_PICKUP, DUMMY_DEST,
//            DUMMY_PASSENGER_NAME, DUMMY_PASSENGER_PHONE, DUMMY_TIME, DUMMY_PRICE, DUMMY_NOTES,
//            DUMMY_STATUS);
    private RecyclerView previous_requests;
    private RecyclerView.Adapter RVadapter;
    private RecyclerView.LayoutManager layoutManager;

//    @Override
//    public void recyclerViewListClicked(View v, int position){
//        Toast.makeText(this,"Everything is awesome", Toast.LENGTH_SHORT).show();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_requests);
        previous_requests = (RecyclerView) findViewById(R.id.future_requests);
        previous_requests.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        previous_requests.setLayoutManager(layoutManager);
  //      OngoingRequestAdapter ca = new OngoingRequestAdapter(this, recyclerViewListClicked, createList(5));
        OngoingRequestAdapter ca = new OngoingRequestAdapter(createList(5), this);
        previous_requests.setAdapter(ca);
//        ca.setOnItemClickListener(this.setOnItemClick);

    }

    private List<request> createList(int size) {

        List<request> result = new ArrayList<request>();
        for (int i=1; i <= size; i++) {
            request ci = new request(DUMMY_REQUEST_ID, DUMMY_PICKUP, DUMMY_DEST,DUMMY_PASSENGER_NAME,
                    DUMMY_PASSENGER_PHONE, DUMMY_TIME, DUMMY_PRICE, DUMMY_NOTES, DUMMY_STATUS);
//            ci.passenger_name = DUMMY_PASSENGER_NAME + i;
//            ci.passenger_phone = DUMMY_PASSENGER_PHONE + i;
//            ci.status = DUMMY_STATUS;
//            ci.time = DUMMY_TIME + i;
//            ci.dest[0] = Double.parseDouble(DUMMY_DEST.split(",")[0]);
//            ci.dest[1] = Double.parseDouble(DUMMY_DEST.split(",")[1]);
//            ci.pickup[0] = Double.parseDouble(DUMMY_PICKUP.split(",")[0]);
//            ci.pickup[1] = Double.parseDouble(DUMMY_PICKUP.split(",")[1]);
            result.add(ci);

        }

        return result;
    }


}
