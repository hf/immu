package me.stojan.immu.compiler.element;

import me.stojan.immu.annotation.Immu;
import me.stojan.immu.annotation.SuperImmu;
import me.stojan.immu.compiler.element.predicate.ImmuPredicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by vuk on 05/01/17.
 */
public class ImmuObjectElement extends ImmuElement {

  public static final ImmuPredicate<ImmuObjectElement> OBJECT_IS_INTERFACE =
      (env, prop) -> ImmuValidationMessages.fromPredicateResult(prop.typeElement().getKind().isInterface(), ImmuValidationMessages.elementNotInterface(prop));

  public static final ImmuPredicate<ImmuObjectElement> EMPTY_SUPERINTERFACES =
      (env, prop) -> prop.superInterfaces()
          .stream()
          .map((iface) -> checkSuperInterface(env, prop.typeElement().asType(), iface))
          .reduce(new ArrayList<>(), (a, l) -> {
            a.addAll(l);
            return a;
          });

  public static final List<ImmuPredicate<ImmuObjectElement>> PREDICATES = Arrays.asList(
      OBJECT_IS_INTERFACE,
      EMPTY_SUPERINTERFACES);

  public static ImmuObjectElement from(Element element) {
    return new ImmuObjectElement(element);
  }

  ImmuObjectElement(Element element) {
    super(element);
  }

  @Override
  public List<String> validate(ProcessingEnvironment environment) {
    return runPredicates(environment, this, PREDICATES);
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

  /**
   * Returns a list of all of the super properties of this element in order starting from the most super type.
   * @param env the environment, must not be null
   * @return the properties, must not be null
   */
  public List<ImmuProperty> superProperties(ProcessingEnvironment env) {
    final TypeElement typeElement = (TypeElement) element;

    final List<? extends TypeMirror> interfaces = typeElement.getInterfaces();

    final List<ImmuProperty> properties = new LinkedList<>();

    for (TypeMirror typeMirror : interfaces) {
      properties.addAll(superProperties(env, typeMirror));
    }

    return properties;
  }

  /**
   * Returns a list of the super-properties, i.e. properties inherited from the extending tree of {@code iface}.
   * @param env the environment, must not be null
   * @param iface the @Immu/@SuperImmu interface for which to recursively find the properties, must not be null
   * @return the list of properties, in order from the most super interface to the provided one
   */
  private List<ImmuProperty> superProperties(ProcessingEnvironment env, TypeMirror iface) {
    final LinkedList<ImmuProperty> properties = new LinkedList<>();

    final Element ifaceElement = env.getTypeUtils().asElement(iface);

    final TypeElement typeElement = (TypeElement) ifaceElement;

    for (TypeMirror mirror : typeElement.getInterfaces()) {
      properties.addAll(superProperties(env, mirror));
    }

    for (Element element : ifaceElement.getEnclosedElements()) {
      if (ElementKind.METHOD.equals(element.getKind())) {
        properties.add(ImmuProperty.from(element));
      }
    }

    return properties;
  }

  /**
   * Returns the type element.
   * @return the element, never null
   */
  public TypeElement typeElement() {
    return (TypeElement) element;
  }

  /**
   * Recursively check the superinterfaces of the provided interface.
   * @param env the environment, must not be null
   * @param extendingIface the interface that extends {@code iface}, must not be null
   * @param iface the interface to check, must not be null
   * @return a list of all the errors, never null
   */
  public static List<String> checkSuperInterface(ProcessingEnvironment env, TypeMirror extendingIface, TypeMirror iface) {
    final Element extendingIFaceElement = env.getTypeUtils().asElement(extendingIface);
    final Element ifaceElement = env.getTypeUtils().asElement(iface);

    if (null != ifaceElement.getAnnotation(SuperImmu.class)) {
      // interface is a @SuperImmu, therefore it will be checked soon (or was already checked)
      return Collections.emptyList();
    }

    if (null != ifaceElement.getAnnotation(Immu.class)) {
      // interface already has an @Immu annotation, therefore will be checked soon (or was already checked)
      // also, this means that we can extend the superinterface's properties
      return Collections.emptyList();
    }

    final List<String> errors = new LinkedList<>();

    for (Element enclosedElement : ifaceElement.getEnclosedElements()) {
      if (ElementKind.METHOD.equals(enclosedElement.getKind())) {
        errors.addAll(ImmuValidationMessages.nonImmuInterfaceHasMethod(extendingIFaceElement, ifaceElement, enclosedElement));
      }
    }

    final TypeElement typeElement = (TypeElement) ifaceElement;

    final List<? extends TypeMirror> superInterfaces = typeElement.getInterfaces();

    for (TypeMirror superIface : superInterfaces) {
      errors.addAll(checkSuperInterface(env, iface, superIface));
    }

    return errors;
  }
}
