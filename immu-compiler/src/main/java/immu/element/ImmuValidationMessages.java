package immu.element;

import immu.SuperImmu;

import javax.lang.model.element.Element;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Contains all validation messages.
 */
public final class ImmuValidationMessages {

  ImmuValidationMessages() {
    throw new UnsupportedOperationException();
  }


  private static String formatInterface(ImmuProperty property, String format, Object... arguments) {
    return formatInterface(property.element().getEnclosingElement(), format, arguments);
  }

  private static String formatInterface(Element iface, String format, Object... arguments) {
    final String immuName;

    if (null != iface.getAnnotation(SuperImmu.class)) {
      immuName = "@SuperImmu";
    } else {
      immuName = "@Immu";
    }

    return String.format(null, String.format((Locale) null, "%s interface %s %s", immuName, iface.getSimpleName(), format), arguments);
  }

  public static List<String> propertyHasParameters(ImmuProperty property) {
    return Collections.singletonList(formatInterface(property, "has method %s with parameters; @Immu or @SuperImmu interfaces must not have methods with parameters", property.name()));
  }

  public static List<String> propertyThrows(ImmuProperty property) {
    return Collections.singletonList(formatInterface(property, "has method %s that throws; @Immu or @SuperImmu interfaces must not have methods that throw", property.name()));
  }

  public static List<String> propertyHasTypeVariables(ImmuProperty property) {
    return Collections.singletonList(formatInterface(property, "has method %s with type variables; @Immu or @SuperImmu interfaces must not have methods with type variables", property.name()));
  }

  public static List<String> propertyReturnsVoid(ImmuProperty property) {
    return Collections.singletonList(formatInterface(property, "has method %s that returns void; @Immu or @SuperImmu interfaces must not have methods that return void", property.name()));
  }

  public static List<String> elementNotInterface(ImmuObjectElement object) {
    final String immuName;

    if (null != object.element().getAnnotation(SuperImmu.class)) {
      immuName = "@SuperImmu";
    } else {
      immuName = "@Immu";
    }

    return Collections.singletonList(String.format((Locale) null, "%s-annotated element %s is not an interface; @Immu or @SuperImmu is only allowed on interfaces", immuName, object.name()));
  }

  public static List<String> nonImmuInterfaceHasMethod(Element extendingIface, Element iface, Element method) {
    return Collections.singletonList(String.format((Locale) null, "%s extends %s, a non-@Immu interface with a method %s#%s(...); @Immu or @SuperImmu interfaces may only extend non-@Immu interfaces without methods", extendingIface.getSimpleName(), iface.getSimpleName(), iface.getSimpleName(), method.getSimpleName()));
  }

  public static List<String> immuAndSuperImmu(ImmuObjectElement element) {
    return Collections.singletonList(String.format((Locale) null, "%s is annotated with @Immu and @SuperImmu at the same time, consider using one of them", element.name()));
  }
}
