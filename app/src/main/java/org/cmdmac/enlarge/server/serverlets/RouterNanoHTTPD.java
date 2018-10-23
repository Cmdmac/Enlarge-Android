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
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.cmdmac.enlarge.server.ControllerInject;
import org.cmdmac.enlarge.server.annotations.Controller;
import org.cmdmac.enlarge.server.annotations.Param;
import org.cmdmac.enlarge.server.annotations.RequestMapping;
import org.cmdmac.enlarge.server.handlers.DefaultHandler;
import org.cmdmac.enlarge.server.handlers.StaticPageHandler;
import org.cmdmac.enlarge.server.processor.IRouter;
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

    public static abstract class BaseRouter implements IRouter {
        protected Collection<RouterMatcher> mappings;

        public BaseRouter() {
            mappings = new ArrayList<>();
        }

        @Override
        public void addRoute(String url, /*int priority,*/ Class<?> handler) {
            if (url != null) {
                mappings.add(new RouterMatcher(url, /*priority + mappings.size(),*/ handler));
            }
        }

        @Override
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

        public Response process(IHTTPSession session) {
            String work = DefaultHandler.normalizeUri(session.getUri());
            Map<String, String> params = null;
            for (RouterMatcher u : mappings) {
                params = u.match(work);
                if (params != null) {
                    return u.process(params, session);
                }
            }
            return null;
        }
    }

    public static class UriRouter extends BaseRouter {

        RouterMatcher mStaticMatcher = new RouterMatcher("", StaticPageHandler.class);
        public UriRouter() {
            super();
            //add controllers
            ControllerInject.inject(this);
//            addRoute(StaticPageHandler.class);
            mappings.add(mStaticMatcher);

        }

        public void addRoute(ControllerMatcher.RequestMappingParams requestMappingParams) {
            RouterMatcher resource = new ControllerMatcher(requestMappingParams);
            mappings.add(resource);
        }

        @Override
        public void addRoute(Class<?> controller) {
//            super.addRoute(handler);
// has controller annotation
            if (controller.isAnnotationPresent(Controller.class)) {
                Controller controllerAnnotation = controller.getAnnotation(Controller.class);
                String name = controllerAnnotation.name();
                boolean needPermissionControl = controllerAnnotation.needPermissonControl();
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
                        ControllerMatcher.RequestMappingParams requestMappingParams = new ControllerMatcher.RequestMappingParams();
                        requestMappingParams.path = fullPath;
                        requestMappingParams.handler = controller;
                        requestMappingParams.method = m;
                        requestMappingParams.methodReflect = method;
                        requestMappingParams.params = params;
                        requestMappingParams.needPermissionControl = needPermissionControl;
                        addRoute(requestMappingParams);
                    }
                }
            }
        }

        public void addRoute(String url, Class<?> handler) {
            mappings.add(new RouterMatcher(url, handler));
        }

        @Override
        public Response process(IHTTPSession session) {
            Response response = super.process(session);
            if (response == null) {
                return mStaticMatcher.process(null, session);
            } else {
                return response;
            }
        }
    }

    private UriRouter router;

    public RouterNanoHTTPD(int port) {
        super(port);
        router = new UriRouter();
    }

    public RouterNanoHTTPD(String hostname, int port) {
        super(hostname, port);
        router = new UriRouter();
    }

    public void addRoute(String url, Class<?> handler) {
        router.addRoute(url, handler);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Try to find match
        return router.process(session);
    }
}
