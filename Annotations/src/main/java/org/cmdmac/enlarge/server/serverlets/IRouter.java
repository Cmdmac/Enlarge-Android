package org.cmdmac.enlarge.server.serverlets;

public interface IRouter {
    void addRoute(Class<?> handler);
    void addRoute(String url, Class<?> handler);
    void removeRoute(String url);
}