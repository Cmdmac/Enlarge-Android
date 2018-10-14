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

import java.io.IOException;

import org.cmdmac.enlarge.server.apps.filemanager.FileManagerHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.util.ServerRunner;

public class AppNanolets extends RouterNanoHTTPD {

    private static boolean ENABLE_REMOTE_CONNECT = false;
    private static final int PORT = 9090;

    private static class AppRouter extends UriRouter {
        RemoteConnectListener listener;
        public AppRouter(RemoteConnectListener listener) {
            this.listener = listener;
        }

        @Override
        public Response process(IHTTPSession session) {
            if (listener == null || !listener.isConnectAllow(session.getUri())) {
                return Response.newFixedLengthResponse("not allow");
            }
            return super.process(session);
        }
    }

    /**
     * Create the server instance
     */
    public AppNanolets(RemoteConnectListener listener) throws IOException {
        super(PORT, new AppRouter(listener));
        addMappings();
        System.out.println("\nRunning! Point your browers to http://localhost:" + PORT + "/ \n");
    }

    public interface RemoteConnectListener {
        boolean isConnectAllow(String uri);
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

    public static void start(RemoteConnectListener listener) {
        try {
            new AppNanolets(listener).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void enableRemoteConnect() {
        ENABLE_REMOTE_CONNECT = true;
    }

    public static boolean isEnableRemoteConnect() {
        return ENABLE_REMOTE_CONNECT;
    }

}
