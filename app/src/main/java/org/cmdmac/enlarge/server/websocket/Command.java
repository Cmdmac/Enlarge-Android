package org.cmdmac.enlarge.server.websocket;

public class Command {
    public static final int PING = 0;
    public static final int PONG = 1;

    public static final int REQUEST_PERMISSION = 100;

    int type;
    String msg;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
