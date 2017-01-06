package me.stojan.immu.compiler.element;

import me.stojan.immu.annotation.Immu;
import me.stojan.immu.annotation.SuperImmu;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by vuk on 05/01/17.
 */
public class ImmuObjectElement extends ImmuElement {

  public static ImmuObjectElement from(Element element) {
    return new ImmuObjectElement(element);
  }

  ImmuObjectElement(Element element) {
    super(element);
  }

  @Override
  public boolean validate(ProcessingEnvironment env) {
    final boolean isInterface = element.getKind().isInterface();
    final boolean isAbstractClass = element.getKind().isClass() && element.getModifiers().contains(Modifier.ABSTRACT);

    boolean isValid = true;

    if (!isInterface) {
      isValid = false;
      error(env, "@Immu annotations may only be used on interfaces");
    }

    final TypeElement typeElement = (TypeElement) element;
    final List<? extends TypeMirror> interfaces = typeElement.getInterfaces();

    for (TypeMirror iface : interfaces) {
      isValid = checkSuperInterface(env, iface) && isValid;
    }

    return properties()
        .stream()
        .reduce(isValid, (a, p) -> p.validate(env) && a, (a, b) -> a && b);
  }

  public List<? extends TypeMirror> superInterfaces() {
    return ((TypeElement) element).getInterfaces();
  }

  public List<ImmuProperty> properties() {
    return methods()
        .stream()
        .map(ImmuProperty::from)
        .collect(Collectors.toList());
  }

  public List<ImmuProperty> superProperties(ProcessingEnvironment env) {
    final TypeElement typeElement = (TypeElement) element;

    final List<? extends TypeMirror> interfaces = typeElement.getInterfaces();

    final List<ImmuProperty> properties = new LinkedList<>();

    for (TypeMirror typeMirror : interfaces) {
      properties.addAll(superProperties(env, typeMirror));
    }

    return properties;
  }

  private List<ImmuProperty> superProperties(ProcessingEnvironment env, TypeMirror iface) {
    final LinkedList<ImmuProperty> properties = new LinkedList<>();

    final Element ifaceElement = env.getTypeUtils().asElement(iface);

    for (Element element : ifaceElement.getEnclosedElements()) {
      if (ElementKind.METHOD.equals(element.getKind())) {
        properties.add(ImmuProperty.from(element));
      }
    }

    final TypeElement typeElement = (TypeElement) ifaceElement;

    for (TypeMirror mirror : typeElement.getInterfaces()) {
      properties.addAll(superProperties(env, mirror));
    }

    return properties;
  }

  public TypeElement typeElement() {
    return (TypeElement) element;
  }

  public boolean checkSuperInterface(ProcessingEnvironment env, TypeMirror iface) {
    final Element ifaceElement = env.getTypeUtils().asElement(iface);

    if (null != ifaceElement.getAnnotation(SuperImmu.class)) {
      // interface is a @SuperImmu, therefore it will be checked soon (or was already checked)
      return true;
    }

    if (null != ifaceElement.getAnnotation(Immu.class)) {
      // interface already has an @Immu annotation, therefore will be checked soon (or was already checked)
      // also, this means that we can extend the superinterface's properties
      return true;
    }

    boolean isValid = true;

    for (Element enclosedElement : ifaceElement.getEnclosedElements()) {
      if (ElementKind.METHOD.equals(enclosedElement.getKind())) {
        isValid = false;
        error(env, "@Immu interfaces may only extend non-@Immu or non-@SuperImmu interfaces without methods; " + ifaceElement.getSimpleName() + " has at least one method");
      }
    }

    final TypeElement typeElement = (TypeElement) ifaceElement;

    final List<? extends TypeMirror> superInterfaces = typeElement.getInterfaces();

    for (TypeMirror superIface : superInterfaces) {
      isValid = checkSuperInterface(env, superIface) && isValid;
    }

    return isValid;
  }
}
