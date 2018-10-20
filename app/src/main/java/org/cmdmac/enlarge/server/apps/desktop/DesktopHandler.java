package org.cmdmac.enlarge.server.apps.desktop;

import android.net.Uri;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.enlarge.server.RouterNanoHTTPD;
import org.cmdmac.enlarge.server.annotation.Controller;
import org.cmdmac.enlarge.server.annotation.DesktopApp;
import org.cmdmac.enlarge.server.annotation.RequestMapping;
import org.cmdmac.enlarge.server.handlers.DefaultHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by fengzhiping on 2018/10/20.
 */

@Controller(name = "desktop")
public class DesktopHandler {
//    public Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
//        String uri = session.getUri();
//        Map<String, List<String>> params = session.getParameters();
//        Uri u = Uri.parse(uri);
//        String op = u.getLastPathSegment();
//        if ("getApps".equals(op)) {
//            return getApps();
//        }
//        return super.get(session);
//    }

    @RequestMapping(path = "getApps")
    public Response getApps() {

        JSONArray json = new JSONArray();
        try {
            for (int i = 0; i < AppNanolets.DESKTOP_APPS.length; i++) {
                Class<?> cls = AppNanolets.DESKTOP_APPS[i];
                DesktopApp desktopApp = cls.getAnnotation(DesktopApp.class);
                JSONObject object = new JSONObject();
                object.put("name", desktopApp.name());
                object.put("icon", desktopApp.icon());
                json.put(i, object);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Response response = Response.newFixedLengthResponse(Status.OK, "application/json", json.toString());
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }
}
