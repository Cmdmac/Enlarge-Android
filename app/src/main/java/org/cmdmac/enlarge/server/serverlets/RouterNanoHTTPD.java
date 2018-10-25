package org.cmdmac.enlarge.server.serverlets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

//import org.cmdmac.enlarge.server.ControllerInject;
import org.cmdmac.enlarge.server.ControllerInject;
import org.cmdmac.enlarge.server.handlers.DefaultHandler;
import org.cmdmac.enlarge.server.handlers.StaticPageHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.websockets.NanoWSD;

/**
 * @author cmdmac
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

        @Override
        public void addRoute(Class<?> handler) {
            mappings.add(new ControllerMatcher("/", handler));
        }

        public void addRoute(String url, Class<?> handler) {
            mappings.add(new ControllerMatcher(url, handler));
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
