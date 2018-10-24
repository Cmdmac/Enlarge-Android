package org.cmdmac.enlarge.server.processor;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

public interface BaseController {
    Response get(IHTTPSession session) throws IllegalAccessException, InstantiationException;
}
