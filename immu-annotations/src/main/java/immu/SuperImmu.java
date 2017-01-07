package immu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used on interfaces that are super-immutable.
 * <p>
 * Super-immutable is similar to a super-class: a container for a common set of immutable
 * properties, but in and of itself does not represent an instantiable immutable object.
 *
 * @see Immu
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE })
public @interface SuperImmu {
}
