package org.cmdmac.enlarge.server.handlers;

/**
 * Created by fengzhiping on 2018/10/20.
 */

import org.cmdmac.enlarge.server.serverlets.RouterMatcher;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;

import java.util.Map;

/**
 * General nanolet to inherit from if you provide text or html data, only
 * fixed size responses will be generated.
 */
public abstract class DefaultHandler {
    public abstract String getText();
    public abstract String getMimeType();
    public abstract IStatus getStatus();

    public Response get(RouterMatcher routerMatcher, Map<String, String> urlParams, IHTTPSession session) {
        return Response.newFixedLengthResponse(getStatus(), getMimeType(), getText());
    }

    public static String normalizeUri(String value) {
        if (value == null) {
            return value;
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;

    }
}