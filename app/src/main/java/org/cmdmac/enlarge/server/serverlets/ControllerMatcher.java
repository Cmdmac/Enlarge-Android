package org.cmdmac.enlarge.server.serverlets;

import android.text.TextUtils;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.enlarge.server.annotations.Param;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ControllerMatcher extends RouterMatcher {
    public static class RequestMappingParams {
        public String path;
        public Class<?> handler;
        public org.nanohttpd.protocols.http.request.Method method;
        public Method methodReflect;
        public ArrayList<Param> params;
        public boolean needPermissionControl = true;
    }

    private RequestMappingParams requestMappingParams;

    public ControllerMatcher(RequestMappingParams requestMappingParams) {
        super(requestMappingParams.path, requestMappingParams.handler);
        this.requestMappingParams = requestMappingParams;
    }

    @Override
    public Response process(Map<String, String> urlParams, IHTTPSession session) {
        if (handler != null){
            try {
                Object object = handler.newInstance();
                return processController(object, urlParams, session);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return super.process(urlParams, session);
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

    public static Object valueToObject(Type t, String v) {
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

    public static Object valueToObject(Class t, String v) {
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
//    if (requestMappingParams != null) {
//        return processController(object, urlParams, session);
//    } else
}
