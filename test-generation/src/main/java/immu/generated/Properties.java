package immu.generated;

import immu.Immu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Immu
public interface Properties {

  int propertyInt();
  byte propertyByte();
  short propertyShort();
  boolean propertyBoolean();
  char propertyChar();
  long propertyLong();
  float propertyFloat();
  double propertyDouble();
  int[] propertyIntArray();
  int[][] propertyIntMatrix();
  ArrayList<? extends Collection> propertyListOfWildcardCollection();
}
