package org.cmdmac.enlarge.server.pocessor;

import com.google.auto.service.AutoService;

import org.cmdmac.enlarge.server.annotations.Controller;
import org.cmdmac.enlarge.server.annotations.RequestMapping;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class ControllerProcessor extends AbstractProcessor {
    private Filer mFiler;
    private Messager mMessager;
    private Elements mElementUtils;
    private ControllerGroupedClass classes = new ControllerGroupedClass();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mMessager = processingEnvironment.getMessager();
        mElementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Controller.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        note("process");
        try {
            // Scan classes
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Controller.class)) {

                // Check if a class has been annotated with @Factory
                if (annotatedElement.getKind() != ElementKind.CLASS) {
                    throw new ProcessingException(annotatedElement, "Only classes can be annotated with @%s",
                            Controller.class.getSimpleName());
                }

                // We can cast it, because we know that it of ElementKind.CLASS
                TypeElement typeElement = (TypeElement) annotatedElement;

                ControllerAnnotatedClass annotatedClass = new ControllerAnnotatedClass(typeElement);

//                checkValidClass(annotatedClass);

                note("haha=" + annotatedClass.getName());
                // Checks if id is conflicting with another @Factory annotated class with the same id
                classes.add(annotatedClass);
            }

            // Generate code
            classes.generateCode(mElementUtils, mFiler);
            classes.clear();
            return true;
        } catch (ProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }



    private void note(String msg) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void note(String format, Object... args) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, String.format(format, args));
    }
}
