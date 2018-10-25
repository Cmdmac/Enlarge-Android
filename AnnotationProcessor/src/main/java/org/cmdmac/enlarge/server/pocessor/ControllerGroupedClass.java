package org.cmdmac.enlarge.server.pocessor;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.cmdmac.enlarge.server.annotations.Controller;
import org.cmdmac.enlarge.server.annotations.RequestMapping;
import org.cmdmac.enlarge.server.processor.IRouter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.logging.Filter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

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

        //generate each handler class
        for (ControllerAnnotatedClass annotatedClass : list) {

            annotatedClass.generateCode(roundEnv, elementUtils, filer, messager);
            annotatedClass.generateInjectCode(method, elementUtils, messager);
        }

        TypeSpec typeSpec = TypeSpec.classBuilder("ControllerInject").addModifiers(Modifier.PUBLIC).addMethod(method.build()).build();
//         Write file
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }
}
