package org.cmdmac.enlarge.server;

/*
 * #%L
 * NanoHttpd-Samples
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cmdmac.enlarge.server.annotation.Controller;
import org.cmdmac.enlarge.server.annotation.Param;
import org.cmdmac.enlarge.server.annotation.RequestMapping;
import org.cmdmac.enlarge.server.handlers.DefaultHandler;
import org.cmdmac.enlarge.server.handlers.Error404UriHandler;
import org.cmdmac.enlarge.server.handlers.IndexHandler;
import org.cmdmac.enlarge.server.handlers.NotImplementedHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.websockets.NanoWSD;

/**
 * @author vnnv
 * @author ritchieGitHub
 */
public abstract class RouterNanoHTTPD extends NanoWSD {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(RouterNanoHTTPD.class.getName());

    public static class UriResource implements Comparable<UriResource> {

        private static final Pattern PARAM_PATTERN = Pattern.compile("(?<=(^|/)):[a-zA-Z0-9_-]+(?=(/|$))");

        private static final String PARAM_MATCHER = "([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=\\s]+)";

        private static final Map<String, String> EMPTY = Collections.unmodifiableMap(new HashMap<String, String>());

        private final String uri;

        private final Pattern uriPattern;

        private int priority;

        private final Class<?> handler;

        private RequestMappingParams requestMappingParams;

        private final Object[] initParameter;

        private final List<String> uriParams = new ArrayList<String>();

        public UriResource(String uri, int priority, Class<?> handler, Object... initParameter) {
            this(uri, handler, initParameter);
            this.priority = priority + uriParams.size() * 1000;
        }

        public UriResource(String uri, Class<?> handler, Object... initParameter) {
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

        public UriResource(RequestMappingParams requestMappingParams) {
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
                        responder.get(this, urlParams, session);
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
            LOG.severe("init parameter index not available " + parameterIndex);
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
        public int compareTo(UriResource that) {
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

    public static interface IRoutePrioritizer {

        void addRoute(String url, int priority, Class<?> handler, Object... initParameter);

        void removeRoute(String url);

        void addController(Class<?> controller);

        Collection<UriResource> getPrioritizedRoutes();

        void setNotImplemented(Class<?> notImplemented);
    }

    public static class RequestMappingParams {
        public String path;
        public Class<?> handler;
        public org.nanohttpd.protocols.http.request.Method method;
        public Method methodReflect;
        public ArrayList<Param> params;
    }

    public static abstract class BaseRoutePrioritizer implements IRoutePrioritizer {

        protected Class<?> notImplemented;

        protected final Collection<UriResource> mappings;

        public BaseRoutePrioritizer() {
            this.mappings = newMappingCollection();
            this.notImplemented = NotImplementedHandler.class;
        }

        @Override
        public void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            if (url != null) {
                if (handler != null) {
                    mappings.add(new UriResource(url, priority + mappings.size(), handler, initParameter));
                } else {
                    mappings.add(new UriResource(url, priority + mappings.size(), notImplemented));
                }
            }
        }

        public void addRoute(RequestMappingParams requestMappingParams) {
            UriResource resource = new UriResource(requestMappingParams);
            mappings.add(resource);
        }

        public void removeRoute(String url) {
            String uriToDelete = DefaultHandler.normalizeUri(url);
            Iterator<UriResource> iter = mappings.iterator();
            while (iter.hasNext()) {
                UriResource uriResource = iter.next();
                if (uriToDelete.equals(uriResource.getUri())) {
                    iter.remove();
                    break;
                }
            }
        }

        @Override
        public Collection<UriResource> getPrioritizedRoutes() {
            return Collections.unmodifiableCollection(mappings);
        }

        @Override
        public void setNotImplemented(Class<?> handler) {
            notImplemented = handler;
        }

        protected abstract Collection<UriResource> newMappingCollection();

        @Override
        public void addController(Class<?> controller) {
            // has controller annotation
            if (controller.isAnnotationPresent(Controller.class)) {
                Controller controllerAnnotation = controller.getAnnotation(Controller.class);
                String name = controllerAnnotation.name();
                Method[] methods = controller.getDeclaredMethods();
                // get all request mapping annotation
                for (Method method : methods) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                        String path = requestMapping.path();
                        org.nanohttpd.protocols.http.request.Method m = requestMapping.method();
                        // build full path
                        String fullPath = name + File.separatorChar + path;
                        // getparams
                        ArrayList<Param> params = new ArrayList<>();
                        Annotation[][] paramAnnotation = method.getParameterAnnotations();
                        for (Annotation[] an : paramAnnotation) {
                            if (an.length > 0) {
                                Param p = (Param)an[0];
                                params.add(p);
                            }
                        }
                        RequestMappingParams requestMappingParams = new RequestMappingParams();
                        requestMappingParams.path = fullPath;
                        requestMappingParams.handler = controller;
                        requestMappingParams.method = m;
                        requestMappingParams.methodReflect = method;
                        requestMappingParams.params = params;
                        addRoute(requestMappingParams);
                    }
                }
            }
        }
    }

    public static class ProvidedPriorityRoutePrioritizer extends BaseRoutePrioritizer {

        @Override
        public void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            if (url != null) {
                UriResource resource = null;
                if (handler != null) {
                    resource = new UriResource(url, handler, initParameter);
                } else {
                    resource = new UriResource(url, handler, notImplemented);
                }

                resource.setPriority(priority);
                mappings.add(resource);
            }
        }

        @Override
        protected Collection<UriResource> newMappingCollection() {
            return new PriorityQueue<UriResource>();
        }

    }

    public static class DefaultRoutePrioritizer extends BaseRoutePrioritizer {

        protected Collection<UriResource> newMappingCollection() {
            return new PriorityQueue<UriResource>();
        }

    }

    public static class InsertionOrderRoutePrioritizer extends BaseRoutePrioritizer {

        protected Collection<UriResource> newMappingCollection() {
            return new ArrayList<UriResource>();
        }
    }

    public static class UriRouter {

        private UriResource error404Url;

        private IRoutePrioritizer routePrioritizer;

        public UriRouter() {
            this.routePrioritizer = new DefaultRoutePrioritizer();
        }

        /**
         * Search in the mappings if the given url matches some of the rules If
         * there are more than one marches returns the rule with less parameters
         * e.g. mapping 1 = /user/:id mapping 2 = /user/help if the incoming uri
         * is www.example.com/user/help - mapping 2 is returned if the incoming
         * uri is www.example.com/user/3232 - mapping 1 is returned
         * 
         * @param url
         * @return
         */
        public Response process(IHTTPSession session) {
            String work = DefaultHandler.normalizeUri(session.getUri());
            Map<String, String> params = null;
            UriResource uriResource = error404Url;
            for (UriResource u : routePrioritizer.getPrioritizedRoutes()) {
                params = u.match(work);
                if (params != null) {
                    uriResource = u;
                    break;
                }
            }
            return uriResource.process(params, session);
        }

        private void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            routePrioritizer.addRoute(url, priority, handler, initParameter);
        }

        private void removeRoute(String url) {
            routePrioritizer.removeRoute(url);
        }

        public void addController(Class<?> controller) {
            routePrioritizer.addController(controller);
        }

        public void setNotFoundHandler(Class<?> handler) {
            error404Url = new UriResource(null, 100, handler);
        }

        public void setNotImplemented(Class<?> handler) {
            routePrioritizer.setNotImplemented(handler);
        }

        public void setRoutePrioritizer(IRoutePrioritizer routePrioritizer) {
            this.routePrioritizer = routePrioritizer;
        }

    }

    private UriRouter router;

    public RouterNanoHTTPD(int port, UriRouter router) {
        super(port);
//        router = new UriRouter();
        this.router = router;
    }

    public RouterNanoHTTPD(String hostname, int port, UriRouter router) {
        super(hostname, port);
//        router = new UriRouter();
        this.router = router;
    }

    /**
     * default routings, they are over writable.
     * 
     * <pre>
     * router.setNotFoundHandler(GeneralHandler.class);
     * </pre>
     */

    public void addMappings() {
        router.setNotImplemented(NotImplementedHandler.class);
        router.setNotFoundHandler(Error404UriHandler.class);
        router.addRoute("/", Integer.MAX_VALUE / 2, IndexHandler.class);
        router.addRoute("/index.html", Integer.MAX_VALUE / 2, IndexHandler.class);
    }

    public void addRoute(String url, Class<?> handler, Object... initParameter) {
        router.addRoute(url, 100, handler, initParameter);
    }

    public void addController(Class<?> controller) {
        router.addController(controller);
    }

    public <T extends DefaultHandler> void setNotImplementedHandler(Class<T> handler) {
        router.setNotImplemented(handler);
    }

    public <T extends DefaultHandler> void setNotFoundHandler(Class<T> handler) {
        router.setNotFoundHandler(handler);
    }

    public void removeRoute(String url) {
        router.removeRoute(url);
    }

    public void setRoutePrioritizer(IRoutePrioritizer routePrioritizer) {
        router.setRoutePrioritizer(routePrioritizer);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Try to find match
        return router.process(session);
    }
}
