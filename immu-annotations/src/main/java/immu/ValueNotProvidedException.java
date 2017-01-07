package immu;

import java.util.Locale;

/**
 * An exception denoting that a value was not provided for a property
 * annotated with {@link Required}.
 */
public class ValueNotProvidedException extends RuntimeException {

  private final String name;

  /**
   * Construct a new exception for the provided property name.
   * @param name the property name, must not be null
   */
  public ValueNotProvidedException(String name) {
    super(String.format((Locale) null, "Value for property %s was not provided", name));
    this.name = name;
  }

  /**
   * Returns the property name for which a value was not provided.
   * @return the name, never null
   */
  public String name() {
    return name;
  }
}
