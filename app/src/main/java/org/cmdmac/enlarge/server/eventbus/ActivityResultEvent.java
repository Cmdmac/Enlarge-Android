package org.cmdmac.enlarge.server.eventbus;

import android.content.Intent;

public class ActivityResultEvent {
    public int requestCode;
    public int resultCode;
    public Intent data;
    public ActivityResultEvent(int requestCode, int resultCode, Intent data) {
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.data = data;
    }

}
