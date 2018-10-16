package org.cmdmac.enlarge.server.websocket;

/*
 * #%L
 * NanoHttpd-Websocket
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

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.IOException;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.rx.Consumer;
import org.cmdmac.rx.Observable;
import org.cmdmac.rx.observable.ObservableEmitter;
import org.cmdmac.rx.observable.ObservableOnSubscribe;
import org.cmdmac.rx.scheduler.Schedulers;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;

/**
 * Created by fengzhiping on 2018/10/14.
 */


public class EnlargeWebSocket extends WebSocket {

    private final AppNanolets server;
    private AppNanolets.PermissionProcesser permissionProcesser;

    public EnlargeWebSocket(AppNanolets server, IHTTPSession handshakeRequest, AppNanolets.PermissionProcesser permissionProcesser) {
        super(handshakeRequest);
        this.server = server;
        this.permissionProcesser = permissionProcesser;
    }


    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
//            if (server.debug) {
//                System.out.println("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " + (code != null ? code : "UnknownCloseCode[" + code + "]")
//                        + (reason != null && !reason.isEmpty() ? ": " + reason : ""));
//            }
        Log.e(EnlargeWebSocket.class.getSimpleName(), "onClose" + " code=" + code + ",reason=" + reason);
    }

    @Override
    protected void onMessage(WebSocketFrame message) {
        try {
            String text = message.getTextPayload();
            Command command = JSON.parseObject(text, Command.class);
            process(command, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void process(Command command, WebSocketFrame message) throws IOException {
        message.setUnmasked();
        switch (command.type) {
            case 0:
                //ping
                command.type = Command.PONG;
                command.msg = "pong";
                message.setTextPayload(JSON.toJSONString(command));
                sendFrame(message);
                break;
            case 100:
                String remote = getHandshakeRequest().getRemoteIpAddress();
                if (!permissionProcesser.isPermissionAllow(remote)&& !this.permissionProcesser.isRequesting()) {
                    this.permissionProcesser.requestPermission(remote, this);
//                    command.type = Command.REQUEST_PERMISSION;
//                    command.msg = "requesting";
//                    message.setTextPayload(JSON.toJSONString(command));
//                    sendFrame(message);
                }
                break;
            default:
//                message.setTextPayload(JSON.toJSONString(command));
                break;
        }
    }

    @Override
    protected void onPong(WebSocketFrame pong) {
//            if (server.debug) {
//                System.out.println("P " + pong);
//            }
    }

    @Override
    protected void onException(IOException exception) {
//            DebugWebSocketServer.LOG.log(Level.SEVERE, "exception occured", exception);
    }

    @Override
    protected void debugFrameReceived(WebSocketFrame frame) {
//            if (server.debug) {
                System.out.println("R " + frame);
//            }
    }

    @Override
    protected void debugFrameSent(WebSocketFrame frame) {
//            if (server.debug) {
                System.out.println("S " + frame);
//            }
    }
}
