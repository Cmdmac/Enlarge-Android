package org.cmdmac.enlarge.server.serverlets;

import android.text.TextUtils;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.enlarge.server.annotations.Param;
import org.cmdmac.enlarge.server.processor.BaseController;
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

    public ControllerMatcher(String path, Class<?> handler) {
        super(path, handler);
    }

    @Override
    public Response process(Map<String, String> urlParams, IHTTPSession session) {
        if (handler != null){
            try {
                BaseController object = (BaseController)handler.newInstance();
                return object.get(session);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return super.process(urlParams, session);
    }
}
