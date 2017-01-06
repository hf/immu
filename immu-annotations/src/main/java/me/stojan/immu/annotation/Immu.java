package me.stojan.immu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used on interfaces that are immutable objects.
 * <p>
 * These interfaces are only allowed to have methods of the form: {@tt ReturnType name()}.
 * The methods must not use generic type variables or throw any exceptions.
 * <p>
 * An interface annotated by this annotation may extend other interfaces that have either
 * no methods, or are themselves annotated as {@link Immu} or {@link SuperImmu}.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE })
public @interface Immu {
}
