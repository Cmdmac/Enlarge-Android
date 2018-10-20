package org.cmdmac.enlarge.server.handlers;

/**
 * Created by fengzhiping on 2018/10/20.
 */

import org.cmdmac.enlarge.server.serverlets.RouterMatcher;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.util.Map;

/**
 * Handling index
 */
public class IndexHandler extends DefaultHandler {
    @Override
    public String getText() {
            return "<html><body><h2>Hello world!</h3></body></html>";
    }

    @Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }

    @Override
    public Response get(RouterMatcher routerMatcher, Map<String, String> urlParams, IHTTPSession session) {
        return super.get(routerMatcher, urlParams, session);
    }
}