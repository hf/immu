package immu.generated;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class PropertiesBuilderTest {

  @Test
  public void exerciseFrom() throws Exception {
    final Properties properties = PropertiesBuilder.create()
        .propertyIntArray(new int[] { 1, 2, 3 })
        .propertyIntMatrix(new int[][] { {1}, {2}, {3} })
        .propertyChar('A')
        .propertyInt(0xF)
        .propertyByte((byte) 0xA)
        .propertyShort((short) 0xB)
        .propertyBoolean(true)
        .propertyLong(123)
        .propertyFloat(1.23f)
        .propertyDouble(1.234)
        .propertyListOfWildcardCollection(new ArrayList<Collection>())
        .build();

    final Properties copy = PropertiesBuilder.from(properties).build();

    assertNotSame(properties, copy);
    assertEquals(properties, copy);
  }

}
