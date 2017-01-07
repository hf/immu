package immu;

import org.junit.Test;

import static org.junit.Assert.*;

public class ValueNotProvidedExceptionTest {

  @Test
  public void constructor() throws Exception {
    final ValueNotProvidedException exception = new ValueNotProvidedException("name");

    assertEquals("Value for property name was not provided", exception.getMessage());
    assertEquals("Value for property name was not provided", exception.getLocalizedMessage());
  }

  @Test
  public void name() throws Exception {
    final ValueNotProvidedException exception = new ValueNotProvidedException("name");

    assertEquals("name", exception.name());
  }

}
