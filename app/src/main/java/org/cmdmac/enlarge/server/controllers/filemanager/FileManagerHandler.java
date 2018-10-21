package org.cmdmac.enlarge.server.controllers.filemanager;

import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.cmdmac.enlarge.server.annotation.Controller;
import org.cmdmac.enlarge.server.annotation.DesktopApp;
import org.cmdmac.enlarge.server.annotation.Param;
import org.cmdmac.enlarge.server.annotation.RequestMapping;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.*;
import java.io.File;

/**
 * Created by fengzhiping on 2018/10/12.
 */
@Controller(name = "filemanager")
@DesktopApp(name = "FileManager", icon = "images/ic_launcher_document.png")
public class FileManagerHandler {
//    @Override
//    public String getMimeType() {
//        return "application/json";
//    }
//
//    @Override
//    public String getText() {
//        return null;
//    }
//
//    @Override
//    public IStatus getStatus() {
//        return Status.OK;
//    }
//
//    @Override
//    public Response get(RouterNanoHTTPD.RouterMatcher uriResource, Map<String, String> urlParams, IHTTPSession session) {
//        return get(session);
//    }
//
//    @Override
//    public Response get(IHTTPSession session) {
//        String uri = session.getUri();
//        Map<String, List<String>> params = session.getParameters();
//        Uri u = Uri.parse(uri);
//        String op = u.getLastPathSegment();
//        if ("list".equals(op)) {
//            if (params.containsKey("dir")) {
//
//                List<String> dirs = params.get("dir");
//                if (dirs.size() > 0 && !TextUtils.isEmpty(dirs.get(0))) {
//                    return list(dirs.get(0));
//                } else {
//                    java.io.File dir = Environment.getExternalStorageDirectory();
//                    return list(dir.getAbsolutePath());
//                }
//            }
//        } else if ("getThumb".equals(op)) {
//            List<String> dirs = params.get("path");
//            if (dirs.size() > 0 && !TextUtils.isEmpty(dirs.get(0))) {
//                return getThumb(dirs.get(0));
//            }
//        } else if ("mkDir".equals(op)) {
//            List<String> dirs = params.get("dir");
//            if (dirs.size() > 0 && !TextUtils.isEmpty(dirs.get(0))) {
//                List<String> dirName = params.get("name");
//                if (dirName.size() > 0 && !TextUtils.isEmpty(dirName.get(0))) {
//                    return mkDir(dirs.get(0), dirName.get(0));
//                }
//            }
//        }
//        return super.get(session);
//    }

    @RequestMapping(path= "list")
    public Response list(@Param(name = "dir", value = "/sdcard") String path) {
        if (TextUtils.isEmpty(path)) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        Dir d = new Dir(new java.io.File(path));
        String json = JSON.toJSONString(d);
        Response response = Response.newFixedLengthResponse(Status.OK, "application/json", json);
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    @RequestMapping(path= "getThumb")
    public Response getThumb(@Param(name = "path") String path) {
        try {
            Bitmap bm = FileUtils.getImageThumbnail(path, 100, 100);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            Response response = Response.newFixedLengthResponse(Status.OK, "image/*", baos.toByteArray());
            response.addHeader("Access-Control-Allow-Origin", "*");
            bm.recycle();
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.newFixedLengthResponse("not found");
    }

    @RequestMapping(path= "mkDir")
    public Response mkDir(@Param(name = "dir") String path, @Param(name = "name") String dirName) {
        java.io.File f = new File(path, dirName);
        JSONObject jsonObject = new JSONObject();
        if (f.mkdir()) {
            jsonObject.put("code", 200);
        } else {
            jsonObject.put("code", 500);
        }
        Response response = Response.newFixedLengthResponse(Status.OK, "application/json", jsonObject.toJSONString());
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

}
