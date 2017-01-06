package me.stojan.immu.compiler.element;

import me.stojan.immu.annotation.NonNull;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Type;

/**
 * Created by vuk on 05/01/17.
 */
public class ImmuProperty extends ImmuElement {

  public static ImmuProperty from(Element element) {
    switch (element.getKind()) {
      case METHOD:
        return new ImmuProperty(element);

      default:
        throw new IllegalArgumentException("Argument element must be a method");
    }
  }

  ImmuProperty(Element method) {
    super(method);
  }

  @Override
  public boolean validate(ProcessingEnvironment env) {
    boolean isValid = true;

    final ExecutableType type = sourceType();

    if (!type.getParameterTypes().isEmpty()) {
      isValid = false;
      error(env, "@Immu methods must not have parameters");
    }

    if (!type.getTypeVariables().isEmpty()) {
      isValid = false;
      error(env, "@Immu methods must not have generic sourceType parameters");
    }

    if (!type.getThrownTypes().isEmpty()) {
      isValid = false;
      error(env, "@Immu methods must not throw exceptions");
    }

    final TypeMirror returnType = type.getReturnType();

    if (TypeKind.VOID.equals(returnType.getKind())) {
      isValid = false;
      error(env, "@Immu methods must not return void");
    }

    if (isPrimitive() && isNonNull()) {
      isValid = false;
      error(env, "@NonNull should not be used for primitive types");
    }

    return isValid;
  }

  public ExecutableType sourceType() {
    return (ExecutableType) element.asType();
  }

  public TypeMirror returnType() {
    return sourceType().getReturnType();
  }

  public boolean isPrimitive() {
    return returnType().getKind().isPrimitive();
  }

  public boolean isNonNull() {
    return null != element.getAnnotation(NonNull.class);
  }
}
