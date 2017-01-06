package me.stojan.immu;

import me.stojan.immu.annotation.Required;
import me.stojan.immu.annotation.SuperImmu;

/**
 * Created by vuk on 06/01/17.
 */
@SuperImmu
@Required
public interface SuperInterface {

  String name();
}
