package com.Wisam.Events;

import android.location.Location;

/**
 * Created by nezuma on 12/15/16.
 */

public class LocationUpdated {
    private Location location;

    public LocationUpdated(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
