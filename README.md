
Enlarge是一个在PC网页端操作手机端数据的工具，目的打造一个开源的可定制化的PC与手机之间数据通信的系统，让你可以轻松就可以拥有AirDroid般的强大功能！

# Enlarge-Android
Enlarge Android Server端,使用NanoHttpd作为http和websocket服务,使用注解处理器自动生成代码,目录结构:

### AnnotationProcessor 注解处理器项目

### Annotation 注解和公用接口定义

### app 主工程
libs - nanohttp, fastjson, rxjavalight库引用目录
 
src/main/assets/dist - Enlarge-Web项目布署目录

src/main/com/google/zxing - 二维码扫描代码

src/main/org/cmdmac/enlarge/server - http和websocket服务代码

##### Http服务入口 : AppNanolets

##### 增加Controller示例
```java
@Controller(name = "filemanager")
//返回给桌面的入口配置
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
```
Controller一键注入入口:
```java
 public static class UriRouter extends BaseRouter {

        RouterMatcher mStaticMatcher = new RouterMatcher("", StaticPageHandler.class);
        public UriRouter() {
            super();
            // 一键注入,否则controller不生效
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
```
    
##### websocket实现 : EnlargeWebSocket

            