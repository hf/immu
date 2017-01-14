package immu;

import java.util.Locale;

/**
 * An exception denoting that a value was not provided for a property
 * annotated with {@link Required}.
 */
public class ValueNotProvidedException extends RuntimeException {

  /**
   * Create an exception for the provided property.
   * @param property the property, must not be null
   * @return the exception, should be thrown
   */
  public static ValueNotProvidedException forProperty(String property) {
    return new ValueNotProvidedException(String.format((Locale) null, "Value for property %s was not provided", property));
  }

  /**
   * Create an exception for multiple properties given as a comma-separated list.
   * @param propertyList the list of property names, must not be null
   * @return the exception, should be thrown
   */
  public static ValueNotProvidedException forProperties(String propertyList) {
    return new ValueNotProvidedException(String.format((Locale) null, "Not all values for the properties %s were provided", propertyList));
  }

  /**
   * Construct a new exception for the provided property name.
   * @param name the property name, must not be null
   */
  ValueNotProvidedException(String message) {
    super(message);
  }
}
