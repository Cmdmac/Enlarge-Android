package org.cmdmac.enlarge.server.handlers;

import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Status;

public class TestHandler extends DefaultHandler {
    @Override
    public String getText() {
        return "test jjj";
    }

    @Override
    public String getMimeType() {
        return "text/plain";
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }
}
