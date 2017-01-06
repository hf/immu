package immu.element;

import immu.Required;
import immu.element.predicate.ImmuPredicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * An @Immu property. This is typically a method without parameters, type variables or exceptions.
 */
public class ImmuProperty extends ImmuElement {

  /** Checks that the property does not have parameters. */
  public static final ImmuPredicate<ImmuProperty> NO_PARAMETERS =
      (env, prop) -> {
        final boolean hasParameters = !prop.sourceType().getParameterTypes().isEmpty();
        return hasParameters ? ImmuPredicate.Result.error(ImmuValidationMessages.propertyHasParameters(prop)) : ImmuPredicate.Result.success();
      };

  /** Checks that the property does not throw exceptions. */
  public static final ImmuPredicate<ImmuProperty> NO_THROWS =
      (env, prop) -> {
        final boolean isThrowing = !prop.sourceType().getThrownTypes().isEmpty();
        return isThrowing ? ImmuPredicate.Result.error(ImmuValidationMessages.propertyThrows(prop)) : ImmuPredicate.Result.success();
      };

  /** Checks that the property does not have generic type variables. */
  public static final ImmuPredicate<ImmuProperty> NO_TYPE_VARIABLES =
      (env, prop) -> {
        final boolean hasTypeVars = !prop.sourceType().getTypeVariables().isEmpty();
        return hasTypeVars ? ImmuPredicate.Result.error(ImmuValidationMessages.propertyHasTypeVariables(prop)) : ImmuPredicate.Result.success();
      };

  /** Checks that the property does not return void. */
  public static final ImmuPredicate<ImmuProperty> NO_RETURN_VOID =
      (env, prop) -> {
        final boolean isVoid = TypeKind.VOID.equals(prop.sourceType().getKind());
        return isVoid ? ImmuPredicate.Result.error(ImmuValidationMessages.propertyReturnsVoid(prop)) : ImmuPredicate.Result.success();
      };

  /** A collection of all of the predicates that need to be applied to a property during validation. */
  public static final List<ImmuPredicate<ImmuProperty>> PREDICATES = Arrays.asList(
      NO_PARAMETERS,
      NO_TYPE_VARIABLES,
      NO_THROWS,
      NO_RETURN_VOID);

  /**
   * Create a property from the element.
   * @param element the element, must be a method, must not be null
   * @return the property, never null
   */
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
  public List<ImmuPredicate.Result> validate(ProcessingEnvironment environment) {
    return runPredicates(environment, this, PREDICATES);
  }

  /**
   * Returns the source type of the property. This is always an {@link ExecutableType}.
   * @return the source type, never null
   */
  public ExecutableType sourceType() {
    return (ExecutableType) element.asType();
  }

  /**
   * Returns the return type of the property.
   * @return the return type mirror, never null
   */
  public TypeMirror returnType() {
    return sourceType().getReturnType();
  }

  /**
   * Checks if the return type of the property is a primitive type.
   * @return if it is a primitive type, never null
   */
  public boolean isPrimitive() {
    return returnType().getKind().isPrimitive();
  }

  /**
   * Checks if the property is marked as {@link @Required}.
   * @return if it is marked as {@link @Required}
   */
  public boolean isRequired() {
    return null != element.getAnnotation(Required.class);
  }
}
