package org.cmdmac.enlarge.server;

/**
 * @Auth cmdmac
 */

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.cmdmac.enlarge.server.eventbus.ConnectedChangeEvent;
import org.cmdmac.enlarge.server.serverlets.RouterNanoHTTPD;
import org.cmdmac.enlarge.server.websocket.Command;
import org.cmdmac.enlarge.server.websocket.EnlargeWebSocket;
import org.cmdmac.rx.Consumer;
import org.cmdmac.rx.Observable;
import org.cmdmac.rx.observable.ObservableEmitter;
import org.cmdmac.rx.observable.ObservableOnSubscribe;
import org.cmdmac.rx.scheduler.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.util.ServerRunner;

public class AppNanolets extends RouterNanoHTTPD {

    private static final int CONNECTION_TIMEOUT = 20 * 1000;
    //    private static boolean ENABLE_REMOTE_CONNECT = false;
    public static final int PORT = 9090;

    private PermissionProcesser permissionProcesser;

    public static class PermissionEntries {
        private static class PermissionItem {
            public String remote;
            public long time;
            public PermissionItem(String remote, long time) {
                this.remote = remote;
                this.time = time;
            }
        }
        private static HashMap<String, PermissionItem> mPermissionMap = new HashMap<>();

        public static String[] getConnectedList() {
            ArrayList<String> list = new ArrayList<>();
            for(Map.Entry<String, PermissionItem> entry : mPermissionMap.entrySet()) {
                list.add(entry.getKey());
            }
            return list.toArray(new String[0]);
        }

        public static void allowRemote(String remote) {
            PermissionItem item = new PermissionItem(remote, System.currentTimeMillis());
            mPermissionMap.put(remote, item);
            EventBus.getDefault().post(new ConnectedChangeEvent(getConnectedList()));
        }

        public static boolean isRemoteAllow(String remote) {
            if (mPermissionMap.containsKey(remote)) {
                PermissionItem item = mPermissionMap.get(remote);
                long t = System.currentTimeMillis() - item.time;
                if (Math.abs(t) > 60 * 30 * 1000) {
                    mPermissionMap.remove(remote);
                    EventBus.getDefault().post(new ConnectedChangeEvent(getConnectedList()));
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession ihttpSession) {
        Log.e(AppNanolets.class.getSimpleName(), "openWebSocket");
        return new EnlargeWebSocket(this, ihttpSession, permissionProcesser);
    }

    /**
     * Create the server instance
     */
    public AppNanolets(PermissionProcesser listener) throws IOException {
        super(PORT);
        addMappings();
        this.permissionProcesser = listener;
        System.out.println("\nRunning! Point your browers to http://localhost:" + PORT + "/ \n");
    }


    public void addMappings() {
//        addRoute("/test", TestHandler.class);
    }

    /**
     * Main entry point
     *
     * @param args
     */
    public static void main(String[] args) {
        ServerRunner.run(AppNanolets.class);
    }

    public static void start(Context context) {
        try {
            new AppNanolets(new PermissionProcesser(context)).start(CONNECTION_TIMEOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //
//    public static void enableRemoteConnect() {
//        ENABLE_REMOTE_CONNECT = true;
//    }
//
//    public static boolean isEnableRemoteConnect() {
//        return ENABLE_REMOTE_CONNECT;
//    }
    public static class PermissionProcesser {

        private boolean mIsRequesting = false;
        private Context mContext;

        public PermissionProcesser(Context context) {
            mContext = context;
        }

        public boolean isRequesting() {
            return mIsRequesting;
        }

        public boolean isPermissionAllow(String remote) {
            return PermissionEntries.isRemoteAllow(remote);
        }

        public void requestPermission(final String remote, final EnlargeWebSocket webSocket) {
            mIsRequesting = true;
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(final ObservableEmitter<Boolean> observableEmitter) {
                    new AlertDialog.Builder(mContext).setMessage("允许" + remote + "访问吗?").setPositiveButton("允许", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.e(PermissionProcesser.class.getSimpleName(), "allow");
                            observableEmitter.onNext(true);
                        }
                    }).setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.e(PermissionProcesser.class.getSimpleName(), "deny");
                            observableEmitter.onNext(false);
                        }
                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            mIsRequesting = false;
                        }
                    }).setTitle("提示").create().show();
                }
            }).subscribeOn(Schedulers.mainThread()).observeOn(Schedulers.newThread()).subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean aBoolean) {
                    mIsRequesting = false;
                    Command command = new Command();
                    command.setType(Command.REQUEST_PERMISSION);
                    if (aBoolean) {
                        //允许
                        PermissionEntries.allowRemote(remote);
                        command.setMsg("allow");
                    } else {
                        command.setMsg("deny");
                    }

                    try {
                        webSocket.send(JSON.toJSONString(command));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
