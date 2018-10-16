package org.cmdmac.enlarge.server;

import android.app.Application;

public class EnalargeApplication extends Application {
    private static EnalargeApplication sInstance;
    public static EnalargeApplication getInstance() {
        return sInstance;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }
}
