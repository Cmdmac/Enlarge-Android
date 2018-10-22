package org.cmdmac.enlarge.server.serverlets;

import android.text.TextUtils;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.enlarge.server.annotations.Param;
import org.cmdmac.enlarge.server.handlers.DefaultHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
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

public class RouterMatcher implements Comparable<RouterMatcher> {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(?<=(^|/)):[a-zA-Z0-9_-]+(?=(/|$))");

    private static final String PARAM_MATCHER = "([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=\\s]+)";

    private static final Map<String, String> EMPTY = Collections.unmodifiableMap(new HashMap<String, String>());

    private final String uri;

    private final Pattern uriPattern;

    private int priority;

    private final Class<?> handler;

    private RouterNanoHTTPD.RequestMappingParams requestMappingParams;

    private final Object[] initParameter;

    private final List<String> uriParams = new ArrayList<String>();

    public RouterMatcher(String uri, int priority, Class<?> handler, Object... initParameter) {
        this(uri, handler, initParameter);
        this.priority = priority + uriParams.size() * 1000;
    }

    public RouterMatcher(String uri, Class<?> handler, Object... initParameter) {
        this.handler = handler;
        this.initParameter = initParameter;
        if (uri != null) {
            this.uri = DefaultHandler.normalizeUri(uri);
            parse();
            this.uriPattern = createUriPattern();
        } else {
            this.uriPattern = null;
            this.uri = null;
        }
    }

    public RouterMatcher(RouterNanoHTTPD.RequestMappingParams requestMappingParams) {
        this(requestMappingParams.path, requestMappingParams.handler);
        this.priority = 100 + uriParams.size() * 1000;
        this.requestMappingParams = requestMappingParams;
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

    private Object valueToObject(Type t, String v) {
        try {
            if (t == int.class) {
                return Integer.parseInt(v);
            } else if (t == long.class) {
                return Long.parseLong(v);
            } else if (t == float.class) {
                return Float.parseFloat(v);
            } else if (t == Integer.class) {
                return Integer.parseInt(v);
            } else if (t == Long.class) {
                return Long.parseLong(v);
            } else if (t == Float.class) {
                return Float.parseFloat(v);
            } else if (t == Long.class) {
                return Long.parseLong(v);
            } else {
                return v;
            }
        } catch (Exception e) {
            return v;
        }
    }

    private Response processController(Object object, Map<String, String> urlParams, IHTTPSession session) throws InvocationTargetException, IllegalAccessException {
        if (requestMappingParams.needPermissionControl) {
            if (!AppNanolets.PermissionEntries.isRemoteAllow(session.getRemoteIpAddress())) {
                return Response.newFixedLengthResponse("not allow");
            }
        }
        if (requestMappingParams.method != session.getMethod()) {
            return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "method not supply");
        }
        ArrayList<Object> params = new ArrayList<>();
        Map<String, List<String>> requestParams = session.getParameters();
        if (requestParams != null) {
            Type[] types = requestMappingParams.methodReflect.getGenericParameterTypes();
            for (int i = 0; i < requestMappingParams.params.size(); i++) {
                Param p = requestMappingParams.params.get(i);
                if (!TextUtils.isEmpty(p.name())) {
                    List<String> values = requestParams.get(p.name());
                    if (values != null && values.size() > 0) {
                        String v = values.get(0);
                        Type t = types[i];
                        params.add(valueToObject(t, v));
                    } else {
                        params.add(p.value());
                    }
                }
            }
        }
        return (Response)requestMappingParams.methodReflect.invoke(object, params.toArray());
    }

    public Response process(Map<String, String> urlParams, IHTTPSession session) {
        String error = "General error!";
        if (handler != null) {
            try {
                Object object = handler.newInstance();
                if (requestMappingParams != null) {
                    return processController(object, urlParams, session);
                } else if (object instanceof DefaultHandler) {
                    DefaultHandler responder = (DefaultHandler) object;
                    return responder.get(this, urlParams, session);
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

    public <T> T initParameter(Class<T> paramClazz) {
        return initParameter(0, paramClazz);
    }

    public <T> T initParameter(int parameterIndex, Class<T> paramClazz) {
        if (initParameter.length > parameterIndex) {
            return paramClazz.cast(initParameter[parameterIndex]);
        }
//        LOG.severe("init parameter index not available " + parameterIndex);
        return null;
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

    @Override
    public int compareTo(RouterMatcher that) {
        if (that == null) {
            return 1;
        } else if (this.priority > that.priority) {
            return 1;
        } else if (this.priority < that.priority) {
            return -1;
        } else {
            return 0;
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}