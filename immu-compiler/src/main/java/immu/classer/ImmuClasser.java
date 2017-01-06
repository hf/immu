package immu.classer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import immu.element.ImmuObjectElement;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * A classer generates classes.
 */
public abstract class ImmuClasser {

  protected final ImmuObjectElement element;
  private final ClassName className;

  /**
   * Construct a classer for the element.
   * @param element the element, must not be null
   */
  protected ImmuClasser(ImmuObjectElement element) {
    this.element = element;
    this.className = ClassName.get(element.typeElement());
  }

  /**
   * Generate a class.
   * @param env the environment, must not be null
   * @return the generated class, never null
   */
  public abstract TypeSpec generate(ProcessingEnvironment env);

  /**
   * Returns the element's class name.
   * @return the name, never null
   */
  public final ClassName className() {
    return className;
  }

  /**
   * Returns the class name for the immutable implementation object.
   * @return the name, never null
   */
  public final ClassName objectClass() {
    return className.peerClass("Immutable" + className.simpleName());
  }

  /**
   * Returns the class name for the builder.
   * @return the name, never null
   */
  public final ClassName builderClass() {
    return className.peerClass(className.simpleName() + "Builder");
  }
}

