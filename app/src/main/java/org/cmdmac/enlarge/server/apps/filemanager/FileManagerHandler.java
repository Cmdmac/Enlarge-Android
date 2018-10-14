package org.cmdmac.enlarge.server.apps.filemanager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.view.TextureView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.cmdmac.enlarge.server.RouterNanoHTTPD;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by fengzhiping on 2018/10/12.
 */

public class FileManagerHandler extends RouterNanoHTTPD.DefaultHandler {
    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }

    @Override
    public Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return get(session);
    }

    @Override
    public Response get(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, List<String>> params = session.getParameters();
        Uri u = Uri.parse(uri);
        String op = u.getLastPathSegment();
        if ("list".equals(op)) {
            if (params.containsKey("dir")) {

                List<String> dirs = params.get("dir");
                if (dirs.size() > 0 && !TextUtils.isEmpty(dirs.get(0))) {
                    return list(dirs.get(0));
                } else {
                    java.io.File dir = Environment.getExternalStorageDirectory();
                    return list(dir.getAbsolutePath());
                }
            }
        } else if ("getThumb".equals(op)) {
            List<String> dirs = params.get("path");
            if (dirs.size() > 0 && !TextUtils.isEmpty(dirs.get(0))) {
                return getThumb(dirs.get(0));
            }
        } else if ("mkDir".equals(op)) {
            List<String> dirs = params.get("dir");
            if (dirs.size() > 0 && !TextUtils.isEmpty(dirs.get(0))) {
                List<String> dirName = params.get("name");
                if (dirName.size() > 0 && !TextUtils.isEmpty(dirName.get(0))) {
                    return mkDir(dirs.get(0), dirName.get(0));
                }
            }
        }
        return super.get(session);
    }

    public Response list(String path) {
        Dir d = new Dir(new java.io.File(path));
        String json = JSON.toJSONString(d);
        Response response = Response.newFixedLengthResponse(getStatus(), getMimeType(), json);
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    public Response getThumb(String path) {
        try {
            Bitmap bm = FileUtils.getImageThumbnail(path, 100, 100);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            Response response = Response.newFixedLengthResponse(getStatus(), "image/*", baos.toByteArray());
            response.addHeader("Access-Control-Allow-Origin", "*");
            bm.recycle();
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.newFixedLengthResponse("not found");
    }

    public Response mkDir(String path, String dirName) {
        java.io.File f = new File(path, dirName);
        JSONObject jsonObject = new JSONObject();
        if (f.mkdir()) {
            jsonObject.put("code", 200);
        } else {
            jsonObject.put("code", 500);
        }
        Response response = Response.newFixedLengthResponse(getStatus(), "application/json", jsonObject.toJSONString());
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }
}
