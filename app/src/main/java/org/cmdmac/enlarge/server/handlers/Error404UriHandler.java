package org.cmdmac.enlarge.server.handlers;

/**
 * Created by fengzhiping on 2018/10/20.
 */

import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Status;

/**
 * Handling error 404 - unrecognized urls
 */
public class Error404UriHandler extends DefaultHandler {

    @Override
    public String getText() {
        return "<html><body><h3>Error 404: the requested page doesn't exist.</h3></body></html>";
    }

    @Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public IStatus getStatus() {
        return Status.NOT_FOUND;
    }
}

