package org.cmdmac.enlarge.server.serverlets;

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

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import org.cmdmac.enlarge.server.annotation.Controller;
import org.cmdmac.enlarge.server.annotation.Param;
import org.cmdmac.enlarge.server.annotation.RequestMapping;
import org.cmdmac.enlarge.server.handlers.DefaultHandler;
import org.cmdmac.enlarge.server.handlers.Error404UriHandler;
import org.cmdmac.enlarge.server.handlers.IndexHandler;
import org.cmdmac.enlarge.server.handlers.NotImplementedHandler;
import org.cmdmac.enlarge.server.handlers.StaticPageHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
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

    public static interface IRoutePrioritizer {

        void addRoute(String url, int priority, Class<?> handler, Object... initParameter);

        void removeRoute(String url);

        void addController(Class<?> controller);

        Collection<RouterMatcher> getPrioritizedRoutes();

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

        protected final Collection<RouterMatcher> mappings;

        public BaseRoutePrioritizer() {
            this.mappings = newMappingCollection();
            this.notImplemented = NotImplementedHandler.class;
        }

        @Override
        public void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            if (url != null) {
                if (handler != null) {
                    mappings.add(new RouterMatcher(url, priority + mappings.size(), handler, initParameter));
                } else {
                    mappings.add(new RouterMatcher(url, priority + mappings.size(), notImplemented));
                }
            }
        }

        public void addRoute(RequestMappingParams requestMappingParams) {
            RouterMatcher resource = new RouterMatcher(requestMappingParams);
            mappings.add(resource);
        }

        public void removeRoute(String url) {
            String uriToDelete = DefaultHandler.normalizeUri(url);
            Iterator<RouterMatcher> iter = mappings.iterator();
            while (iter.hasNext()) {
                RouterMatcher routerMatcher = iter.next();
                if (uriToDelete.equals(routerMatcher.getUri())) {
                    iter.remove();
                    break;
                }
            }
        }

        @Override
        public Collection<RouterMatcher> getPrioritizedRoutes() {
            return Collections.unmodifiableCollection(mappings);
        }

        @Override
        public void setNotImplemented(Class<?> handler) {
            notImplemented = handler;
        }

        protected abstract Collection<RouterMatcher> newMappingCollection();

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
                RouterMatcher resource = null;
                if (handler != null) {
                    resource = new RouterMatcher(url, handler, initParameter);
                } else {
                    resource = new RouterMatcher(url, handler, notImplemented);
                }

                resource.setPriority(priority);
                mappings.add(resource);
            }
        }

        @Override
        protected Collection<RouterMatcher> newMappingCollection() {
            return new PriorityQueue<RouterMatcher>();
        }

    }

    public static class DefaultRoutePrioritizer extends BaseRoutePrioritizer {

        protected Collection<RouterMatcher> newMappingCollection() {
            return new PriorityQueue<RouterMatcher>();
        }

    }

    public static class InsertionOrderRoutePrioritizer extends BaseRoutePrioritizer {

        protected Collection<RouterMatcher> newMappingCollection() {
            return new ArrayList<RouterMatcher>();
        }
    }

    public static class UriRouter {

        private RouterMatcher error404Url;
        private RouterMatcher staticUrl;

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
            RouterMatcher routerMatcher = staticUrl;
            for (RouterMatcher u : routePrioritizer.getPrioritizedRoutes()) {
                params = u.match(work);
                if (params != null) {
                    routerMatcher = u;
                    break;
                }
            }

            return routerMatcher.process(params, session);
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
            error404Url = new RouterMatcher(null, 100, handler);
        }

        public void setStaticHandler(Class<?> handler) {
            staticUrl = new RouterMatcher(StaticPageHandler.ANDDROID_ASSETS_SCHEMA, 100, handler);
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
//        router = new RouterMatcher();
        this.router = router;
    }

    public RouterNanoHTTPD(String hostname, int port, UriRouter router) {
        super(hostname, port);
//        router = new RouterMatcher();
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
        router.setStaticHandler(StaticPageHandler.class);
//        router.addRoute("/", Integer.MAX_VALUE / 2, IndexHandler.class);
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
