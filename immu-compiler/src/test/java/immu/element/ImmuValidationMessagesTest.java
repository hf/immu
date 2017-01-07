package immu.element;

import org.junit.Test;

public class ImmuValidationMessagesTest {

  @Test(expected = UnsupportedOperationException.class)
  public void constructor_noInstances() throws Exception {
    new ImmuValidationMessages();
  }
}
