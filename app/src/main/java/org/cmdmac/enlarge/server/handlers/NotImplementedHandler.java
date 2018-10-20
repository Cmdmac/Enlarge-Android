package org.cmdmac.enlarge.server.handlers;

import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Status;

/**
 * Created by fengzhiping on 2018/10/20.
 */

public class NotImplementedHandler extends DefaultHandler {

    @Override
    public String getText() {
        return "<html><body><h2>The uri is mapped in the router, but no handler is specified. <br> Status: Not implemented!</h3></body></html>";
    }

    @Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }
}
