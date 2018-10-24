package org.cmdmac.enlarge.server.processor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Utils {
    public static String getParam(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        if (values != null && values.size() > 0 && !StringUtils.isEmpty(values.get(0))) {
            return values.get(0);
        }
        return "";
    }

    public static Object valueToObject(Type t, String v) {
        try {
            if (t == int.class) {
                return Integer.parseInt(v);
            } else if (t == long.class) {
                return Long.parseLong(v);
            } else if (t == float.class) {
                return Float.parseFloat(v);
            } else if (t == Integer.class) {
                return Integer.parseInt(v);
            } else if (t == Long.class) {
                return Long.parseLong(v);
            } else if (t == Float.class) {
                return Float.parseFloat(v);
            } else if (t == Long.class) {
                return Long.parseLong(v);
            } else {
                return v;
            }
        } catch (Exception e) {
            return v;
        }
    }
}
