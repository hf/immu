package immu.generated;

import immu.ValueNotProvidedException;
import org.junit.Test;

public class RequiredPropertiesLazyCheckedTest {

  @Test(expected = ValueNotProvidedException.class)
  public void notProvidedProperties() throws Exception {
    RequiredPropertiesBuilder.create().build();
  }
}
