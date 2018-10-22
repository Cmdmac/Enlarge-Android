package org.cmdmac.enlarge.server.pocessor;


import org.cmdmac.enlarge.server.annotations.Controller;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

public class ControllerAnnotatedClass {

    private TypeElement annotatedClassElement;
    private String qualifiedClassName;
    private String simpleName;
    private String name;
    private boolean needPermissionControll;

    /**
     * @throws ProcessingException if id() from annotation is null
     */
    public ControllerAnnotatedClass(TypeElement classElement) throws ProcessingException {
        this.annotatedClassElement = classElement;
        Controller annotation = classElement.getAnnotation(Controller.class);
        name = annotation.name();
        needPermissionControll = annotation.needPermissonControl();

        if (StringUtils.isEmpty(name)) {
            throw new ProcessingException(classElement,
                    "name() in @%s for class %s is null or empty! that's not allowed",
                    Controller.class.getSimpleName(), classElement.getQualifiedName().toString());
        }

        // Get the full QualifiedTypeName
        try {
            Class<?> clazz = annotation.getClass();
            qualifiedClassName = clazz.getCanonicalName();
            simpleName = clazz.getSimpleName();
        } catch (MirroredTypeException mte) {
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            qualifiedClassName = classTypeElement.getQualifiedName().toString();
            simpleName = classTypeElement.getSimpleName().toString();
        }
    }

    /**
     * Get the id as specified in {@link Controller#name()}.
     * return the name
     */
    public String getName() {
        return name;
    }

    public boolean needPermissionControll() {
        return needPermissionControll;
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
}
