package me.stojan.immu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used on methods in {@link Immu} and {@link SuperImmu} annotated interfaces to
 * denote non-null properties. Those properties will never have null values.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface NonNull {
}
