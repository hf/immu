package me.stojan.immu.compiler.element;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by vuk on 05/01/17.
 */
public abstract class ImmuElement {

  protected final Element element;

  protected ImmuElement(Element element) {
    this.element = element;
  }

  public final Element element() {
    return element;
  }

  public abstract boolean validate(ProcessingEnvironment env);

  protected final void error(ProcessingEnvironment env, String message) {
    env.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  protected final List<Element> methods() {
    return filter(ElementKind.METHOD);
  }

  protected final List<Element> classes() {
    return filter(ElementKind.CLASS);
  }

  private List<Element> filter(ElementKind kind) {
    return element.getEnclosedElements()
        .stream()
        .filter(element -> kind.equals(element.getKind()))
        .collect(Collectors.toList());
  }

  public final Name name() {
    return element.getSimpleName();
  }
}
