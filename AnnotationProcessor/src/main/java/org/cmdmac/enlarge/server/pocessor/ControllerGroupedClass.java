package org.cmdmac.enlarge.server.pocessor;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.cmdmac.enlarge.server.processor.IRouter;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
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

    public void clear() {
        list.clear();
    }

    public void generateCode(Elements elementUtils, Filer filer) throws IOException {

        String packageName = "org.cmdmac.enlarge.server";//pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();

        MethodSpec.Builder method = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(IRouter.class, "router")
                .returns(TypeName.get(void.class));

        // check if id is null
//        method.beginControlFlow("if (id == null)")
//                .addStatement("throw new IllegalArgumentException($S)", "id is null!")
//                .endControlFlow();

        for (ControllerAnnotatedClass annotatedClass : list) {
            method.addStatement("router.addRoute(" + annotatedClass.getTypeElement() + ".class)");
        }
        TypeSpec typeSpec = TypeSpec.classBuilder("ControllerInject").addModifiers(Modifier.PUBLIC).addMethod(method.build()).build();
        // Write file
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }
}
