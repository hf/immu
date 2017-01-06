package me.stojan.immu.compiler.element.predicate;

import me.stojan.immu.compiler.element.ImmuElement;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.List;

/**
 * A predicate on an {@link ImmuElement}.
 */
public interface ImmuPredicate<E extends ImmuElement> {

  /**
   * Returns an optional string if the predicate did not apply.
   *
   * @param env
   * @param element the element on which to apply the predicate, must not be null
   * @return the optional string, never null
   */
  List<String> apply(ProcessingEnvironment env, E element);
}
