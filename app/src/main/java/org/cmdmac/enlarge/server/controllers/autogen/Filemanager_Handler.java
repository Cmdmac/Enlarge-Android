package org.cmdmac.enlarge.server.controllers.autogen;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.enlarge.server.controllers.filemanager.FileManagerHandler;
import org.cmdmac.enlarge.server.processor.BaseController;
import org.cmdmac.enlarge.server.processor.Utils;
import org.cmdmac.enlarge.server.serverlets.ControllerMatcher;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.util.List;
import java.util.Map;

public class Filemanager_Handler implements BaseController {
    Class<? extends FileManagerHandler> cls = FileManagerHandler.class;
    public Filemanager_Handler(Class<? extends FileManagerHandler> cls) {
        this.cls = cls;
    }


//    public Response invoke_list(Map<String, List<String>> params) throws IllegalAccessException, InstantiationException {
//        FileManagerHandler object = cls.newInstance();
//        String v1 = (String) ControllerMatcher.valueToObject(String.class, Utils.getParam(params, "dir"));
//        return object.list(v1);
//    }

    public Response get(IHTTPSession session) throws IllegalAccessException, InstantiationException {
        if (!AppNanolets.PermissionEntries.isRemoteAllow(session.getRemoteIpAddress())) {
            return Response.newFixedLengthResponse("not allow");
        }
        if (Method.GET != session.getMethod()) {
            return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "method not supply");
        }
        String uri = session.getUri();
        Map<String, List<String>> params = session.getParameters();
        String path = uri.substring(uri.indexOf('/') + 1);
//        if ("list".equals(path)) {
//            return invoke_list(params);
//        } else if ("getThumb".equals(path)) {
//            List<String> dirs = params.get("path");
//            if (dirs.size() > 0 && !TextUtils.isEmpty(dirs.get(0))) {
//                return getThumb(dirs.get(0));
//            }
//        } else if ("mkDir".equals(path)) {
//            String v1 = (String) ControllerMatcher.valueToObject(String.class, Utils.getParam(params, "dir"));
//            String v2 = (String) ControllerMatcher.valueToObject(String.class, Utils.getParam(params, "name"));
//            return mkDir(v1, v2);
//        }
//        return super.get(session);
        return null;
    }
}
