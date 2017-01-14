package immu;

import org.junit.Test;

import static org.junit.Assert.*;

public class ValueNotProvidedExceptionTest {

  @Test
  public void constructor() throws Exception {
    final ValueNotProvidedException exception = new ValueNotProvidedException("name");

    assertEquals("name", exception.getMessage());
    assertEquals("name", exception.getLocalizedMessage());
  }

  @Test
  public void forProperty() throws Exception {
    final ValueNotProvidedException exception = ValueNotProvidedException.forProperty("name");

    assertTrue(exception.getMessage().contains("name"));
    assertTrue(exception.getLocalizedMessage().contains("name"));
  }

  @Test
  public void forProperties() throws Exception {
    final ValueNotProvidedException exception = ValueNotProvidedException.forProperties("name1, name2");

    assertTrue(exception.getMessage().contains("name1, name2"));
    assertTrue(exception.getLocalizedMessage().contains("name1, name2"));
  }

  @Test
  public void name() throws Exception {
    final ValueNotProvidedException exception = new ValueNotProvidedException("name");

    assertTrue(exception.getMessage().contains("name"));
  }

}
