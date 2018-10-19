package org.cmdmac.enlarge.server;

/*
 * #%L
 * NanoHttpd-Samples
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

/**
 * Created by vnnv on 7/17/15.
 * Simple httpd server based on NanoHTTPD
 * Read the source. Everything is there.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cmdmac.enlarge.server.apps.filemanager.FileManagerHandler;
import org.cmdmac.enlarge.server.websocket.Command;
import org.cmdmac.enlarge.server.websocket.EnlargeWebSocket;
import org.cmdmac.rx.Consumer;
import org.cmdmac.rx.Observable;
import org.cmdmac.rx.observable.ObservableEmitter;
import org.cmdmac.rx.observable.ObservableOnSubscribe;
import org.cmdmac.rx.scheduler.Schedulers;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
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

        public static void setPermissionChangeListener(OnPermissonChange listener) {
            sPermisisonChangeListeners = listener;
        }

        private static OnPermissonChange sPermisisonChangeListeners = null;

        public static interface OnPermissonChange {
            void onChange(String[] connectedList);
        }

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
            if (sPermisisonChangeListeners != null) {
                sPermisisonChangeListeners.onChange(getConnectedList());
            }
        }

        public static boolean isRemoteAllow(String remote) {
            if (mPermissionMap.containsKey(remote)) {
                PermissionItem item = mPermissionMap.get(remote);
                long t = System.currentTimeMillis() - item.time;
                if (Math.abs(t) > 60 * 30 * 1000) {
                    mPermissionMap.remove(remote);
                    if (sPermisisonChangeListeners != null) {
                        sPermisisonChangeListeners.onChange(getConnectedList());
                    }
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

    private static class AppRouter extends UriRouter {

        @Override
        public Response process(IHTTPSession session) {
            if (!PermissionEntries.isRemoteAllow(session.getRemoteIpAddress())) {
                return Response.newFixedLengthResponse("not allow");
            }
            return super.process(session);
        }
    }

    /**
     * Create the server instance
     */
    public AppNanolets(PermissionProcesser listener) throws IOException {
        super(PORT, new AppRouter());
        addMappings();
        this.permissionProcesser = listener;
        System.out.println("\nRunning! Point your browers to http://localhost:" + PORT + "/ \n");
    }


    /**
     * Add the routes Every route is an absolute path Parameters starts with ":"
     * Handler class should implement @UriResponder interface If the handler not
     * implement UriResponder interface - toString() is used
     */
    @Override
    public void addMappings() {
        super.addMappings();
        addRoute("/filemanager/list", FileManagerHandler.class);
        addRoute("/filemanager/delete", FileManagerHandler.class);
        addRoute("/filemanager/getThumb", FileManagerHandler.class);
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
