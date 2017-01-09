package immu.generated;

import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertiesEqualsTest {

  @Test
  public void exerciseEqualsWithNull() throws Exception {
    final Properties properties = PropertiesBuilder.create().build();

    assertFalse("V != null", properties.equals(null));
  }

  @Test
  public void exerciseEqualsWithSelf() throws Exception {
    final Properties properties = PropertiesBuilder.create().build();

    assertTrue("V@0 == V@0", properties.equals(properties));
  }

  @Test
  public void exericseEqualsWithNonProperties() throws Exception {
    final Properties properties = PropertiesBuilder.create().build();

    assertFalse("V != String", properties.equals("Hello, World"));
  }

  @Test
  public void exerciseEqualsWithDifferentImplementationOfProperties() throws Exception {
    final Properties properties = PropertiesBuilder.create().build();

    assertFalse("V != Properties", properties.equals(new PropertiesImpl()));
  }

  @Test
  public void exerciseEqualsWithNulls() throws Exception {
    final Properties propertiesA = PropertiesBuilder.create().build();
    final Properties propertiesB = PropertiesBuilder.create().build();

    assertTrue("A == B", propertiesA.equals(propertiesB));
    assertTrue("B == A", propertiesA.equals(propertiesB));
  }

  @Test
  public void exerciseEqualsForIntArray() throws Exception {
    final int[] a = new int[] { 1, 2, 3 };
    final int[] copyOfA = new int[] { 1, 2, 3 };
    final int[] b = new int[] { 4, 5, 6 };

    final Properties propertiesA = PropertiesBuilder.create().propertyIntArray(a).build();
    final Properties propertiesCopyOfA = PropertiesBuilder.create().propertyIntArray(copyOfA).build();
    final Properties propertiesB = PropertiesBuilder.create().propertyIntArray(b).build();
    final Properties propertiesNull = PropertiesBuilder.create().build();

    assertTrue("A == copy(A)", propertiesA.equals(propertiesCopyOfA));
    assertTrue("copy(A) == A", propertiesCopyOfA.equals(propertiesA));
    assertFalse("V != null", propertiesA.equals(propertiesNull));
    assertFalse("null != V", propertiesNull.equals(propertiesA));
    assertFalse("A != B", propertiesA.equals(propertiesB));
    assertFalse("B != A", propertiesB.equals(propertiesA));
  }

  @Test
  public void exerciseEqualsForIntMatrix() throws Exception {
    // testing the use of Arrays.equals NOT Arrays.deepEquals

    final int[][] a = new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
    final int[][] copyOfA = new int[][] { a[0], a[1], a[2] };
    final int[][] b = new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

    final Properties propertiesA = PropertiesBuilder.create().propertyIntMatrix(a).build();
    final Properties propertiesCopyOfA = PropertiesBuilder.create().propertyIntMatrix(copyOfA).build();
    final Properties propertiesB = PropertiesBuilder.create().propertyIntMatrix(b).build();
    final Properties propertiesNull = PropertiesBuilder.create().build();

    assertTrue("A == copy(A)", propertiesA.equals(propertiesCopyOfA));
    assertTrue("copy(A) == A", propertiesCopyOfA.equals(propertiesA));
    assertFalse("V != null", propertiesA.equals(propertiesNull));
    assertFalse("null != V", propertiesNull.equals(propertiesA));
    assertFalse("A != B", propertiesA.equals(propertiesB));
    assertFalse("B != A", propertiesB.equals(propertiesA));
  }

  @Test
  public void exerciseEqualsForListOfWildcardCollections() throws Exception {
    final ArrayList<? extends Collection> a = new ArrayList<Collection>();
    final ArrayList<? extends Collection> copyOfA = new ArrayList<Collection>();
    final ArrayList<? extends Collection> b = new ArrayList<Collection>(Collections.singletonList(Collections.emptyList()));

    final Properties propertiesA = PropertiesBuilder.create().propertyListOfWildcardCollection(a).build();
    final Properties propertiesCopyOfA = PropertiesBuilder.create().propertyListOfWildcardCollection(copyOfA).build();
    final Properties propertiesB = PropertiesBuilder.create().propertyListOfWildcardCollection(b).build();
    final Properties propertiesNull = PropertiesBuilder.create().build();

    assertTrue("A == copy(A)", propertiesA.equals(propertiesCopyOfA));
    assertTrue("copy(A) == A", propertiesCopyOfA.equals(propertiesA));
    assertFalse("V != null", propertiesA.equals(propertiesNull));
    assertFalse("null != V", propertiesNull.equals(propertiesA));
    assertFalse("A != B", propertiesA.equals(propertiesB));
    assertFalse("B != A", propertiesB.equals(propertiesA));
  }

  @Test
  public void exerciseEquals() throws Exception {
    final PropertiesBuilder builder = PropertiesBuilder.create();

    for (Method method : Properties.class.getDeclaredMethods()) {
      final Class<?> returnType = method.getReturnType();
      final Method builderMethod = PropertiesBuilder.class.getMethod(method.getName(), returnType);

      final Properties previous = builder.build();

      if (returnType.isArray()) {
        builderMethod.invoke(builder, Array.newInstance(returnType.getComponentType(), 3));
      } else if (returnType.isPrimitive()) {
        if (int.class.equals(returnType)) {
          builderMethod.invoke(builder, 123);
        } else if (short.class.equals(returnType)) {
          builderMethod.invoke(builder, (short) 123);
        } else if (byte.class.equals(returnType)) {
          builderMethod.invoke(builder, (byte) 123);
        } else if (char.class.equals(returnType)) {
          builderMethod.invoke(builder, (char) 123);
        } else if (float.class.equals(returnType)) {
          builderMethod.invoke(builder, 123.0f);
        } else if (double.class.equals(returnType)) {
          builderMethod.invoke(builder, 123.0);
        } else if (long.class.equals(returnType)) {
          builderMethod.invoke(builder, 123L);
        } else if (boolean.class.equals(returnType)) {
          builderMethod.invoke(builder, true);
        } else {
          throw new RuntimeException("Unhandled primitive " + returnType);
        }
      } else {
        builderMethod.invoke(builder, returnType.newInstance());
      }

      assertFalse(builder.build().equals(previous));
    }
  }
}
