package org.cmdmac.enlarge.server.pocessor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.cmdmac.enlarge.server.annotations.DesktopApp;
import org.cmdmac.enlarge.server.serverlets.IRouter;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class ControllerGroupedClass {

    private ArrayList<ControllerAnnotatedClass> list = new ArrayList<>();

    public ControllerGroupedClass() {
    }

    /**
     * Adds an annotated class to this factory.
     *
     * @throws ProcessingException if another annotated class with the same id is
     *                             already present.
     */
    public void add(ControllerAnnotatedClass toInsert) throws ProcessingException {
        list.add(toInsert);
    }

    public int size() {
        return list.size();
    }

    public void clear() {
        list.clear();
    }

    public void generateCode(RoundEnvironment roundEnv, Elements elementUtils, Filer filer, Messager messager) throws IOException {
        if (list.size() <= 0) {
            return;
        }

        String packageName = "org.cmdmac.enlarge.server";//pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();

        MethodSpec.Builder method = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(IRouter.class, "router")
                .returns(TypeName.get(void.class));

        // desktop config
        FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(Class[].class, "DESKTOP_APPS")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC);

        StringBuilder sb = new StringBuilder();
        sb.append("new Class[] {");
        //generate each handler class
        for (ControllerAnnotatedClass annotatedClass : list) {
            TypeElement typeElement = annotatedClass.getTypeElement();
            DesktopApp desktopApp = typeElement.getAnnotation(DesktopApp.class);
            if (desktopApp != null) {
                sb.append(annotatedClass.getQualifiedName()).append(".class").append(',');
            }
            annotatedClass.generateCode(roundEnv, elementUtils, filer, messager);
            annotatedClass.generateInjectCode(method, elementUtils, messager);
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append('}');
        fieldSpecBuilder.initializer(sb.toString());

        TypeSpec typeSpec = TypeSpec.classBuilder("ControllerInject")
                .addModifiers(Modifier.PUBLIC)
                .addField(fieldSpecBuilder.build())
                .addMethod(method.build()).build();
//         Write file
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }
}
