package org.cmdmac.enlarge.server.handlers;

import org.cmdmac.enlarge.server.RouterNanoHTTPD;
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

    public Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        String baseUri = uriResource.getUri();
        String realUri = normalizeUri(session.getUri());
        for (int index = 0; index < Math.min(baseUri.length(), realUri.length()); index++) {
            if (baseUri.charAt(index) != realUri.charAt(index)) {
                realUri = normalizeUri(realUri.substring(index));
                break;
            }
        }
        File fileOrdirectory = uriResource.initParameter(File.class);
        for (String pathPart : getPathArray(realUri)) {
            fileOrdirectory = new File(fileOrdirectory, pathPart);
        }
        if (fileOrdirectory.isDirectory()) {
            fileOrdirectory = new File(fileOrdirectory, "index.html");
            if (!fileOrdirectory.exists()) {
                fileOrdirectory = new File(fileOrdirectory.getParentFile(), "index.htm");
            }
        }
        if (!fileOrdirectory.exists() || !fileOrdirectory.isFile()) {
            return new Error404UriHandler().get(uriResource, urlParams, session);
        } else {
            try {
                return Response.newChunkedResponse(getStatus(), getMimeTypeForFile(fileOrdirectory.getName()), fileToInputStream(fileOrdirectory));
            } catch (IOException ioe) {
                return Response.newFixedLengthResponse(Status.REQUEST_TIMEOUT, "text/plain", (String) null);
            }
        }
    }

    protected BufferedInputStream fileToInputStream(File fileOrdirectory) throws IOException {
        return new BufferedInputStream(new FileInputStream(fileOrdirectory));
    }
}
