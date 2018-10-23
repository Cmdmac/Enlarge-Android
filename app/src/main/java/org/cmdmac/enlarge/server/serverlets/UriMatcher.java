package org.cmdmac.enlarge.server.serverlets;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

import java.util.Map;

public interface UriMatcher {
    Response process(Map<String, String> urlParams, IHTTPSession session);
}