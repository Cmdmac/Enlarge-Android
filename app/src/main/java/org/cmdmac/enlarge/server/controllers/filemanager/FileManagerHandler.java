package org.cmdmac.enlarge.server.controllers.filemanager;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.cmdmac.enlarge.server.annotations.Controller;
import org.cmdmac.enlarge.server.annotations.DesktopApp;
import org.cmdmac.enlarge.server.annotations.Param;
import org.cmdmac.enlarge.server.annotations.RequestMapping;
import org.cmdmac.enlarge.server.serverlets.ControllerMatcher;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.*;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by fengzhiping on 2018/10/12.
 */
@Controller(name = "filemanager")
@DesktopApp(name = "FileManager", icon = "images/ic_launcher_document.png")
public class FileManagerHandler {

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

    @RequestMapping(path = "download")
    public Response download(@Param(name = "path") String path) {
        java.io.File f = new File(path);
        if(f.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(f);
                return Response.newFixedLengthResponse(Status.OK, "application/octet-stream", fileInputStream, f.length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return Response.newFixedLengthResponse("not found");
    }

    @RequestMapping(path = "open")
    public Response open(@Param(name = "path") String path) {
        java.io.File f = new File(path);
        if(f.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(f);
                return Response.newFixedLengthResponse(Status.OK, NanoHTTPD.getMimeTypeForFile(path), fileInputStream, f.length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return Response.newFixedLengthResponse("not found");
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

    @RequestMapping(path= "mkDir", method = Method.POST)
    public Response mkDir(@Param(name = "dir") String path, @Param(name = "name") String dirName) {
        java.io.File f = new File(path, dirName);
        JSONObject jsonObject = new JSONObject();
        if (f.exists()) {
            jsonObject.put("code", 500);
            jsonObject.put("message", "already exists!");
        } else if (f.mkdir()){
            jsonObject.put("code", 200);
        } else {
            jsonObject.put("code", 500);
        }
        Response response = Response.newFixedLengthResponse(Status.OK, "application/json", jsonObject.toJSONString());
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    @RequestMapping(path = "upload", method = Method.POST)
    public Response upload(String[] fileNames, String[] tmpFilePaths, String dir) {
        boolean success = true;
        for (int i = 0; i < tmpFilePaths.length; i++) {
            java.io.File f = new File(tmpFilePaths[i]);
            java.io.File of = new File(dir, fileNames[i]);
            if (FileUtils.copy(f.getAbsolutePath(), of.getAbsolutePath()) == false) {
                success = false;
            } else {
//                success = true;
                Log.v(FileManagerHandler.class.getSimpleName(), "upload file=" + fileNames[i] + " success");
            }
//            boolean success = FileUtils.copy(f.getAbsolutePath(), of.getAbsolutePath());
        }
        JSONObject jsonObject = new JSONObject();
        if (success) {
            jsonObject.put("code", 200);
        } else {
            jsonObject.put("code", 500);
            jsonObject.put("message", "upload failure");
        }
        Response response = Response.newFixedLengthResponse(Status.OK, "application/json", jsonObject.toJSONString());
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    @RequestMapping(path= "test")
    public Response test(@Param(name = "dir") String path, @Param(name = "testName") String dirName, int test) {
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
