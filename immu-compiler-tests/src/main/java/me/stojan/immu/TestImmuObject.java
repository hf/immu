package me.stojan.immu;

import immu.Immu;

import java.util.List;

/**
 * Created by vuk on 05/01/17.
 */
@Immu
public interface TestImmuObject extends SuperInterface {

  int value();

  List<String> strings();

  int[] array();

  long reallyLongValue();
}
