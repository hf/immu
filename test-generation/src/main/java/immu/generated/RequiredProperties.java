package immu.generated;

import immu.Immu;
import immu.Required;

@Immu
public interface RequiredProperties {

  @Required
  Object propertyReference();

  @Required
  int propertyPrimitive();

  @Required
  int[] propertyArray();
}
