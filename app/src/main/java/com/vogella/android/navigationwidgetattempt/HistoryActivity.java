package com.vogella.android.navigationwidgetattempt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String DUMMY_REQUEST_ID = "1243";
    private static final String DUMMY_PICKUP = "15.5838046,32.5543825";
    private static final String DUMMY_DEST = "15.8838046, 32.6543825";
    private static final String DUMMY_PASSENGER_NAME = "John Green";
    private static final String DUMMY_PASSENGER_PHONE = "0123456789";
    private static final String DUMMY_STATUS = "on the way";
    private static final String DUMMY_NOTES = "Drive slowly";
    private static final String DUMMY_PRICE = "43";
    private static final String DUMMY_TIME = "06/11/2016 - 15:45";
//    private static request current_request = new request(DUMMY_REQUEST_ID, DUMMY_PICKUP, DUMMY_DEST,
//            DUMMY_PASSENGER_NAME, DUMMY_PASSENGER_PHONE, DUMMY_TIME, DUMMY_PRICE, DUMMY_NOTES,
//            DUMMY_STATUS);
    private RecyclerView previous_requests;
    private RecyclerView.Adapter RVadapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        previous_requests = (RecyclerView) findViewById(R.id.past_requests);
        previous_requests.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        previous_requests.setLayoutManager(layoutManager);
        RequestAdapter ca = new RequestAdapter(createList(30));
        previous_requests.setAdapter(ca);
//        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ongoing_request);
//        relativeLayout.setVisibility(View.INVISIBLE);


    }

    private List<request> createList(int size) {

        List<request> result = new ArrayList<request>();
        for (int i=1; i <= size; i++) {
            request ci = new request();
            ci.passenger_name = DUMMY_PASSENGER_NAME + i;
            ci.passenger_phone = DUMMY_PASSENGER_PHONE + i;
            ci.status = DUMMY_STATUS;
            ci.time = DUMMY_TIME;
            ci.dest[0] = Double.parseDouble(DUMMY_DEST.split(",")[0]);
            ci.dest[1] = Double.parseDouble(DUMMY_DEST.split(",")[1]);
            ci.pickup[0] = Double.parseDouble(DUMMY_PICKUP.split(",")[0]);
            ci.pickup[1] = Double.parseDouble(DUMMY_PICKUP.split(",")[1]);
//            ci.pickup = DUMMY_PICKUP;
            ci.price = DUMMY_PRICE + i;

            result.add(ci);

        }

        return result;
    }

}
