package immu.generated;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PropertiesHashCodeTest {

  private static int defaultHashCode(Class<?> klass) {
    return klass.getCanonicalName().hashCode();
  }

  @Test
  public void exerciseHashCodeWithNulls() throws Exception {
    final Properties properties = PropertiesBuilder.create().build();

    assertEquals(defaultHashCode(Properties.class), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForLong() throws Exception {
    final long longValue = 1234567891011121314L;

    final Properties properties = PropertiesBuilder.create()
        .propertyLong(longValue)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ (int) ((longValue >> 32) ^ (longValue)), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForArray() throws Exception {
    final int[] array = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

    final Properties properties = PropertiesBuilder.create()
        .propertyIntArray(array)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ Arrays.hashCode(array), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForMatrix() throws Exception {
    final int[][] matrix = new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

    final Properties properties = PropertiesBuilder.create()
        .propertyIntMatrix(matrix)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ Arrays.hashCode(matrix), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForDouble() throws Exception {
    final double doubleValue = Double.MAX_VALUE;

    final Properties properties = PropertiesBuilder.create()
        .propertyDouble(doubleValue)
        .build();

    final long bits = Double.doubleToLongBits(doubleValue);

    assertEquals(defaultHashCode(Properties.class) ^ (int) ((bits >> 32) ^ bits), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForFloat() throws Exception {
    final float floatValue = Float.MIN_VALUE;

    final Properties properties = PropertiesBuilder.create()
        .propertyFloat(floatValue)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ Float.floatToIntBits(floatValue), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForByte() throws Exception {
    final byte byteValue = (byte) 0xFC;

    final Properties properties = PropertiesBuilder.create()
        .propertyByte(byteValue)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ ((~0) & byteValue), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForChar() throws Exception {
    final char charValue = 'A';

    final Properties properties = PropertiesBuilder.create()
        .propertyChar(charValue)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ ((~0) & charValue), properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForTrue() throws Exception {
    final boolean booleanValue = true;

    final Properties properties = PropertiesBuilder.create()
        .propertyBoolean(booleanValue)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ 1, properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForFalse() throws Exception {
    final boolean booleanValue = false;

    final Properties properties = PropertiesBuilder.create()
        .propertyBoolean(booleanValue)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ 0, properties.hashCode());
  }

  @Test
  public void exerciseHashCodeForListOfWildcardCollection() throws Exception {
    final ArrayList<List<String>> collection = new ArrayList<List<String>>();

    final Properties properties = PropertiesBuilder.create()
        .propertyListOfWildcardCollection(collection)
        .build();

    assertEquals(defaultHashCode(Properties.class) ^ collection.hashCode(), properties.hashCode());
  }
}
