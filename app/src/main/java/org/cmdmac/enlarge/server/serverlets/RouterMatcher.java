package org.cmdmac.enlarge.server.serverlets;

import org.cmdmac.enlarge.server.handlers.DefaultHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nanohttpd.protocols.http.NanoHTTPD.LOG;

/**
 * Created by fengzhiping on 2018/10/20.
 */

public class RouterMatcher implements /*Comparable<RouterMatcher>,*/ UriMatcher {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(?<=(^|/)):[a-zA-Z0-9_-]+(?=(/|$))");

    private static final String PARAM_MATCHER = "([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=\\s]+)";

    private static final Map<String, String> EMPTY = Collections.unmodifiableMap(new HashMap<String, String>());

    private final String uri;

    private final Pattern uriPattern;

//    private int priority;

    protected final Class<?> handler;

//    private final Object[] initParameter;

    private final List<String> uriParams = new ArrayList<String>();

//    public RouterMatcher(String uri, Class<?> handler) {
//        this(uri, handler);
//        this.priority = priority + uriParams.size() * 1000;
//    }

    public RouterMatcher(String uri, Class<?> handler) {
        this.handler = handler;
//        this.initParameter = initParameter;
        if (uri != null) {
            this.uri = DefaultHandler.normalizeUri(uri);
            parse();
            this.uriPattern = createUriPattern();
        } else {
            this.uriPattern = null;
            this.uri = null;
        }
    }

    private void parse() {
    }

    private Pattern createUriPattern() {
        String patternUri = uri;
        Matcher matcher = PARAM_PATTERN.matcher(patternUri);
        int start = 0;
        while (matcher.find(start)) {
            uriParams.add(patternUri.substring(matcher.start() + 1, matcher.end()));
            patternUri = new StringBuilder(patternUri.substring(0, matcher.start()))//
                    .append(PARAM_MATCHER)//
                    .append(patternUri.substring(matcher.end())).toString();
            start = matcher.start() + PARAM_MATCHER.length();
            matcher = PARAM_PATTERN.matcher(patternUri);
        }
        return Pattern.compile(patternUri);
    }

    @Override
    public Response process(Map<String, String> urlParams, IHTTPSession session) {
        String error = "General error!";
        if (handler != null) {
            try {
                Object object = handler.newInstance();
                if (object instanceof DefaultHandler) {
                    DefaultHandler responder = (DefaultHandler) object;
                    return responder.process(this, urlParams, session);
                } else {
                    return Response.newFixedLengthResponse(Status.OK, "text/plain", //
                            new StringBuilder("Return: ")//
                                    .append(handler.getCanonicalName())//
                                    .append(".toString() -> ")//
                                    .append(object)//
                                    .toString());
                }
            } catch (Exception e) {
                error = "Error: " + e.getClass().getName() + " : " + e.getMessage();
                LOG.log(Level.SEVERE, error, e);
            }
        }
        return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", error);
    }

    @Override
    public String toString() {
        return new StringBuilder("UrlResource{uri='").append((uri == null ? "/" : uri))//
                .append("', urlParts=").append(uriParams)//
                .append('}')//
                .toString();
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> match(String url) {
        Matcher matcher = uriPattern.matcher(url);
        if (matcher.matches()) {
            if (uriParams.size() > 0) {
                Map<String, String> result = new HashMap<String, String>();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    result.put(uriParams.get(i - 1), matcher.group(i));
                }
                return result;
            } else {
                return EMPTY;
            }
        }
        return null;
    }

}