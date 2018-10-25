package org.cmdmac.enlarge.server.pocessor;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.cmdmac.enlarge.server.annotations.Controller;
import org.cmdmac.enlarge.server.annotations.Param;
import org.cmdmac.enlarge.server.annotations.RequestMapping;
import org.cmdmac.enlarge.server.processor.BaseController;
import org.cmdmac.enlarge.server.processor.IRouter;
import org.cmdmac.enlarge.server.processor.StringUtils;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class ControllerAnnotatedClass {

    private TypeElement annotatedClassElement;
    private String qualifiedClassName;
    private String simpleName;

    /**
     * @throws ProcessingException if id() from annotation is null
     */
    public ControllerAnnotatedClass(TypeElement classElement, Messager messager) throws ProcessingException {
        this.annotatedClassElement = classElement;
        this.qualifiedClassName = this.annotatedClassElement.getQualifiedName().toString();
        this.simpleName = this.annotatedClassElement.getSimpleName().toString();
    }

    /**
     * Get the full qualified name of the type specified in  {@link Controller#type()}.
     *
     * @return qualified name
     */
    public String getQualifiedName() {
        return qualifiedClassName;
    }

    /**
     * Get the simple name of the type specified in  {@link Controller#type()}.
     *
     * @return qualified name
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * The original element that was annotated with @Factory
     */
    public TypeElement getTypeElement() {
        return annotatedClassElement;
    }

    /**
     * 生成相关处理类
     * @param roundEnv
     * @param elementUtils
     * @param filer
     * @param messager
     * @throws IOException
     */
    public void generateCode(RoundEnvironment roundEnv, Elements elementUtils, Filer filer, Messager messager) throws IOException {
        TypeElement typeElement = this.annotatedClassElement;

        String fullName = getQualifiedName();
        int index = fullName.lastIndexOf('.');
        String pkgName = fullName.substring(0, index);
        String className = fullName.substring(index + 1);

        // 方法列表
        ArrayList<MethodSpec> methodSpecs = new ArrayList<>();
        // 构造函数
        MethodSpec constructionMethod = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();
        methodSpecs.add(constructionMethod);
//        public Response get(IHTTPSession session) throws IllegalAccessException, InstantiationException {
        // get方法
        MethodSpec.Builder getMethod = MethodSpec.methodBuilder("get")
                .addParameter(IHTTPSession.class, "session")
                .addModifiers(Modifier.PUBLIC)
                .addException(IllegalAccessError.class)
                .addException(IllegalAccessException.class)
                .addException(InstantiationError.class)
                .addException(InstantiationException.class)
                .returns(TypeName.get(Response.class));

        Controller controller = typeElement.getAnnotation(Controller.class);
        if (controller.needPermissonControl()) {
            getMethod.beginControlFlow("if (!org.cmdmac.enlarge.server.AppNanolets.PermissionEntries.isRemoteAllow(session.getRemoteIpAddress())) ")
                    .addStatement("return org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse(\"not allow\")")
                    .endControlFlow();
        }

        getMethod.addStatement("String uri = session.getUri().substring(1)");
        getMethod.addStatement("java.util.Map<String, java.util.List<String>> params = session.getParameters()");
        getMethod.addStatement("String path = uri.substring(uri.indexOf('/') + 1)");

        // find RequestMapping annotation method
        //　生成子方法
        for (Element ee : elementUtils.getAllMembers(typeElement)) {
            ExecutableElement eElement = (ExecutableElement) ee;
            Annotation annotation = eElement.getAnnotation(RequestMapping.class);
            if (annotation != null) {
                RequestMapping requestMapping = (RequestMapping) annotation;
//                messager.printMessage(Diagnostic.Kind.NOTE, eElement.getSimpleName() + "--");

                String invokeAndReturnStatement = String.format("return invoke_%s(params)",eElement.getSimpleName());
                TypeMirror returnType = eElement.getReturnType();

//                messager.printMessage(Diagnostic.Kind.NOTE, returnType.toString() + "--" + Response.class.getCanonicalName());

                // check method return type
                if (!returnType.toString().equals(Response.class.getCanonicalName())) {
                    invokeAndReturnStatement = "return null";
                    continue;
                }

                getMethod.beginControlFlow(String.format("if (\"%s\".equals(path))", requestMapping.path()))
                        .addStatement(invokeAndReturnStatement)
                        .endControlFlow();
                MethodSpec m = generateMethodCode(className, eElement.getSimpleName().toString(),
                        eElement.getParameters(), messager);
                methodSpecs.add(m);
            }
        }

        getMethod.addStatement("return null");
        methodSpecs.add(getMethod.build());

        //创建类,增加方法
        ParameterizedTypeName typeName = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(ClassName.get(pkgName, className)));
        FieldSpec fieldSpec = FieldSpec.builder(typeName, "cls").initializer(getQualifiedName() + ".class").build();
        TypeSpec.Builder classSpec = TypeSpec.classBuilder(className + "_Proxy")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(TypeName.get(BaseController.class))
                .addField(fieldSpec);
        for (MethodSpec methodSpec : methodSpecs) {
            classSpec.addMethod(methodSpec);
        }
        JavaFile.builder(pkgName, classSpec.build()).build().writeTo(filer);
    }

    /**
     * 生成调用方法　
     * @param clsName
     * @param invokeMethodName
     * @param ves
     * @param messager
     * @return
     * @throws IOException
     */
    private MethodSpec generateMethodCode(String clsName, String invokeMethodName, List<? extends VariableElement> ves, Messager messager) throws IOException {

        String methodName = String.format("invoke_%s", invokeMethodName);
        ParameterizedTypeName parameterSpec = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                ParameterizedTypeName.get(List.class, String.class));
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(methodName)
                .addParameter(parameterSpec, "params")
                .addModifiers(Modifier.PUBLIC)
                .addException(IllegalAccessError.class)
                .addException(IllegalAccessException.class)
                .addException(InstantiationError.class)
                .addException(InstantiationException.class)
                .returns(TypeName.get(Response.class));
        methodSpecBuilder.addStatement(String.format("%s object = cls.newInstance()", clsName));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ves.size(); i++) {
            VariableElement ve = ves.get(i);
            Param param = ve.getAnnotation(Param.class);
            String key = ve.getSimpleName().toString();
            if (param != null && !StringUtils.isEmpty(param.name())) {
                key = param.name();
            }
            String p = String.format("v%d", i + 1);
            String s = String.format("%s %s = (%s) org.cmdmac.enlarge.server.processor.Utils.valueToObject(%s.class, org.cmdmac.enlarge.server.processor.Utils.getParam(params, \"%s\"))",
                    ve.asType(), p,  ve.asType(), ve.asType(), key);
            methodSpecBuilder.addStatement(s);
            sb.append(p);
            if (i != ves.size() -1) {
                sb.append(',');
            }
//            messager.printMessage(Diagnostic.Kind.NOTE,  "-" + ve.asType() + "-" + ve.getSimpleName());
        }

        methodSpecBuilder.addStatement(String.format("return object.%s(%s)", invokeMethodName, sb.toString()));

        return methodSpecBuilder.build();
    }

    public void generateInjectCode(MethodSpec.Builder methodSpec, Elements elements, Messager messager) throws IOException {
        messager.printMessage(Diagnostic.Kind.NOTE, "generateInjectCode");

        Controller controller = annotatedClassElement.getAnnotation(Controller.class);
        for (Element ee : elements.getAllMembers(this.annotatedClassElement)) {
            ExecutableElement eElement = (ExecutableElement) ee;
            Annotation annotation = eElement.getAnnotation(RequestMapping.class);
            if (annotation != null) {
//                messager.printMessage(Diagnostic.Kind.NOTE, annotation.toString() + "--");

                RequestMapping requestMapping = (RequestMapping) annotation;
                String fullPath = controller.name() + '/' + requestMapping.path();
                methodSpec.addStatement("router.addRoute(\"" + fullPath + "\", " + this.getQualifiedName() + "_Proxy.class)");
            }
        }

    }
}
