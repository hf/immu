package immu.generated;

import immu.ValueNotProvidedException;
import org.junit.Test;

public class RequiredPropertiesLazyCheckedTest {

  @Test(expected = ValueNotProvidedException.class)
  public void notProvidedProperties() throws Exception {
    RequiredPropertiesBuilder.create().build();
  }

  @Test
  public void exerciseToString() throws Exception {
    RequiredPropertiesBuilder.create()
        .propertyReference(new Object())
        .propertyPrimitive(3)
        .propertyArray(new int[3])
        .build()
        .toString();
  }
}
