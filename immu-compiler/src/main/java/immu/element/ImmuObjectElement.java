package immu.element;

import immu.Immu;
import immu.Immutable;
import immu.SuperImmu;
import immu.element.predicate.ImmuPredicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An element annotated with {@link Immu} or {@link SuperImmu}.
 */
public class ImmuObjectElement extends ImmuElement {

  /** Checks that the annotated object is an interface. */
  public static final ImmuPredicate<ImmuObjectElement> OBJECT_IS_INTERFACE =
      (env, element) -> {
        boolean notInterface;
        switch (element.typeElement().getKind()) {
          case INTERFACE:
            notInterface = false;
            break;

          default:
            notInterface = true;
        }

        return notInterface ? ImmuPredicate.Result.error(ImmuValidationMessages.elementNotInterface(element)) : ImmuPredicate.Result.success();
      };

  /** Checks that if the interface has super-interfaces that are not annotated, they are empty. */
  public static final ImmuPredicate<ImmuObjectElement> EMPTY_SUPERINTERFACES =
      (env, element) -> {
        final List<String> errors = element.superInterfaces()
            .stream()
            .map((iface) -> checkSuperInterface(env, element.typeElement().asType(), iface))
            .reduce(new ArrayList<>(), (a, l) -> {
              a.addAll(l);
              return a;
            });

        if (errors.isEmpty()) {
          return ImmuPredicate.Result.success();
        }

        return ImmuPredicate.Result.error(errors);
      };

  public static final ImmuPredicate<ImmuObjectElement> IMMU_AND_SUPER_IMMU =
      (env, element) -> {
        final boolean bothAnnotations = null != element.element().getAnnotation(Immu.class)
            && null != element.element().getAnnotation(SuperImmu.class);

        return bothAnnotations ? ImmuPredicate.Result.warning(ImmuValidationMessages.immuAndSuperImmu(element)) : ImmuPredicate.Result.success();
      };

  public static final List<ImmuPredicate<ImmuObjectElement>> PREDICATES = Arrays.asList(
      OBJECT_IS_INTERFACE,
      EMPTY_SUPERINTERFACES,
      IMMU_AND_SUPER_IMMU);

  /**
   * Create a new object element from the provided element.
   * @param element the element, must not be null
   * @return the element, never null
   */
  public static ImmuObjectElement from(Element element) {
    return new ImmuObjectElement(element);
  }

  ImmuObjectElement(Element element) {
    super(element);
  }

  @Override
  public List<ImmuPredicate.Result> validate(ProcessingEnvironment environment) {
    final List<ImmuPredicate.Result> results = new ArrayList<>();

    results.addAll(runPredicates(environment, this, PREDICATES));

    superProperties(environment)
        .stream()
        .map((p) -> p.validate(environment))
        .forEach(results::addAll);

    properties()
        .stream()
        .map((p) -> p.validate(environment))
        .forEach(results::addAll);

    return results;
  }

  /**
   * Returns a list of the immediate super-interfaces of the element.
   * @return the list, never null
   */
  public List<? extends TypeMirror> superInterfaces() {
    return ((TypeElement) element).getInterfaces();
  }
  /**
   * Returns a list of the immediate properties in the object.
   * @see #superProperties(ProcessingEnvironment)
   * @return the properties, never null
   */
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
