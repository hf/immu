package immu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used on methods in {@link Immu} and {@link SuperImmu} annotated interfaces to
 * denote a required property. A property is "required" if it has to be set in a builder,
 * i.e. be non-null for non-primitive types.
 * <p>
 * This annotation will have no effect if used on an interface. The processor should warn you.
 * <p>
 * If a value has not been provided, then {@link ValueNotProvidedException}
 * will be thrown either in the constructor of the generated immutable object's implementation,
 * or in the builder.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Required {
}
