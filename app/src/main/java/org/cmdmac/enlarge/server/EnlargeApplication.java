package org.cmdmac.enlarge.server;

import android.app.Application;

public class EnlargeApplication extends Application {
    private static EnlargeApplication sInstance;
    public static EnlargeApplication getInstance() {
        return sInstance;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }
}
