package immu.element.predicate;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImmuPredicateResultTest {

  @Test
  public void error() throws Exception {
    final List<String> errors = Arrays.asList("error1", "error2");

    final ImmuPredicate.Result result = ImmuPredicate.Result.error(errors);

    assertEquals(errors, result.errors);
    assertTrue("No warnings", result.warnings.isEmpty());
  }

  @Test
  public void warning() throws Exception {
    final List<String> warnings = Arrays.asList("warning1", "warning2");

    final ImmuPredicate.Result result = ImmuPredicate.Result.warning(warnings);

    assertEquals(warnings, result.warnings);
    assertTrue("No warnings", result.errors.isEmpty());
  }

  @Test
  public void notSuccess() throws Exception {
    final List<String> errors = Arrays.asList("error1", "error2");
    final List<String> warnings = Arrays.asList("warning1", "warning2");

    final ImmuPredicate.Result result = ImmuPredicate.Result.notSuccess(warnings, errors);

    assertEquals(warnings, result.warnings);
    assertEquals(errors, result.errors);
  }

  @Test
  public void success() throws Exception {
    final ImmuPredicate.Result result = ImmuPredicate.Result.success();

    assertTrue("No warnings", result.warnings.isEmpty());
    assertTrue("No errors", result.errors.isEmpty());
  }

}
