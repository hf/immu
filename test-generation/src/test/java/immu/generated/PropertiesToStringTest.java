package immu.generated;

import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class PropertiesToStringTest {

  @Test
  public void exerciseToString() throws Exception {
    final PropertiesBuilder builder = PropertiesBuilder.create();

    builder.build().toString();

    for (Method method : Properties.class.getDeclaredMethods()) {
      final Class<?> returnType = method.getReturnType();
      final Method builderMethod = PropertiesBuilder.class.getMethod(method.getName(), returnType);

      if (!returnType.isPrimitive()) {
        if (returnType.isArray()) {
          builderMethod.invoke(builder, Array.newInstance(returnType.getComponentType(), 3));
        } else {
          builderMethod.invoke(builder, returnType.newInstance());
        }
      }

      builder.build().toString();
    }
  }

  @Test
  public void toStringContainsProperties() throws Exception {
    final String toString = PropertiesBuilder.create().build().toString();

    for (Method method : Properties.class.getDeclaredMethods()) {
      assertTrue("" + method.getName() + " in toString", Pattern.compile("" + method.getName() + "\\s*=\\s*(\\@null|\\<.*\\>)\\s*(,\\s*)?").matcher(toString).find());
    }
  }

  @Test
  public void toStringContainsImmuName() throws Exception {
    final String toString = PropertiesBuilder.create().build().toString();

    assertTrue("Starts with Immu name", toString.startsWith("Properties@"));
  }
}
