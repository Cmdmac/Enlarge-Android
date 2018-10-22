package org.cmdmac.enlarge.server.annotations;

import org.nanohttpd.protocols.http.request.Method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by fengzhiping on 2018/10/20.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {
    String path() default "";
    Method method() default Method.GET;
}