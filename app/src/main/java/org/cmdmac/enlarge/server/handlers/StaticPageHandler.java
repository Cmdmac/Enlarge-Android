package org.cmdmac.enlarge.server.handlers;

import android.text.TextUtils;

import org.cmdmac.enlarge.server.serverlets.RouterMatcher;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.nanohttpd.protocols.http.NanoHTTPD.getMimeTypeForFile;

/**
 * Created by fengzhiping on 2018/10/20.
 */

/**
 * General nanolet to print debug info's as a html page.
 */
public class StaticPageHandler extends DefaultHandler {

    private static String[] getPathArray(String uri) {
        String array[] = uri.split("/");
        ArrayList<String> pathArray = new ArrayList<String>();

        for (String s : array) {
            if (s.length() > 0)
                pathArray.add(s);
        }

        return pathArray.toArray(new String[]{});

    }

    public String getText() {
        throw new IllegalStateException("this method should not be called");
    }

    public String getMimeType() {
        throw new IllegalStateException("this method should not be called");
    }

    public IStatus getStatus() {
        return Status.OK;
    }

    public Response get(RouterMatcher routerMatcher, Map<String, String> urlParams, IHTTPSession session) {
        String baseUri = routerMatcher.getUri();
        String realUri = normalizeUri(session.getUri());

        if (TextUtils.isEmpty(realUri)) {
            // http://host, find index.html
            File index = new File(baseUri, "index.html");
            if (!index.exists()) {
                // not find index.html, return default IndexHandler
                return new IndexHandler().get(routerMatcher, urlParams, session);
            } else {
                try {
                    return Response.newChunkedResponse(getStatus(), getMimeTypeForFile(index.getName()), fileToInputStream(index));
                } catch (IOException ioe) {
                    return Response.newFixedLengthResponse(Status.REQUEST_TIMEOUT, "text/plain", (String) null);
                }
            }

        } else {
            File f = new File(baseUri, realUri);
            if (!f.exists() || !f.isFile()) {
                return new Error404UriHandler().get(routerMatcher, urlParams, session);
            } else {
                try {
                    return Response.newChunkedResponse(getStatus(), getMimeTypeForFile(f.getName()), fileToInputStream(f));
                } catch (IOException ioe) {
                    return Response.newFixedLengthResponse(Status.REQUEST_TIMEOUT, "text/plain", (String) null);
                }
            }
        }
    }

    protected BufferedInputStream fileToInputStream(File fileOrdirectory) throws IOException {
        return new BufferedInputStream(new FileInputStream(fileOrdirectory));
    }
}
