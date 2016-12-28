package com.vogella.android.navigationwidgetattempt;

import android.app.Application;

/**
 * Created by nezuma on 12/28/16.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FontsOverride.setDefaultFont(this, "DEFAULT", "fonts/Roboto-Light.ttf");
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/Roboto-Light.ttf");
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/Roboto-Light.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/Roboto-Light.ttf");

    }
}
