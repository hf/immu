package me.stojan.immu.compiler.element.predicate;

import me.stojan.immu.compiler.element.ImmuElement;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A predicate on an {@link ImmuElement}.
 */
public interface ImmuPredicate<E extends ImmuElement> {

  /**
   * A predicate's result.
   */
  final class Result {
    /** Predicate's warnings. */
    public final List<String> warnings;

    /** Predicate's errors. */
    public final List<String> errors;

    /**
     * Creates a new success result.
     * @return the result, never null
     */
    public static Result success() {
      return new Result(Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Creates a new error result without warnings.
     * @param errors the errors, must not be null
     * @return the result, never null
     */
    public static Result error(List<String> errors) {
      return notSuccess(Collections.emptyList(), errors);
    }

    /**
     * Creates a new warnings-only result.
     * @param warnings the warnings, must not be null
     * @return the result, never null
     */
    public static Result warning(List<String> warnings) {
      return notSuccess(warnings, Collections.emptyList());
    }

    /**
     * Creates a non-success result.
     * @param warnings the warnings, must not be null
     * @param errors the errors, must not be null
     * @return the result, never null
     */
    public static Result notSuccess(List<String> warnings, List<String> errors) {
      return new Result(new ArrayList<>(warnings), new ArrayList<>(errors));
    }

    Result(List<String> warnings, List<String> errors) {
      this.warnings = Collections.unmodifiableList(warnings);
      this.errors = Collections.unmodifiableList(errors);
    }

    /**
     * Check if the result is a success.
     * @return if it is a success
     */
    public boolean isSuccess() {
      return errors.isEmpty();
    }
  }

  /**
   * Returns an optional string if the predicate did not apply.
   * @param env the processing environment, must not be null
   * @param element the element on which to apply the predicate, must not be null
   * @return the result, never null
   */
  Result apply(ProcessingEnvironment env, E element);
}
