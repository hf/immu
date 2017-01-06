package me.stojan.immu.compiler.element;

import me.stojan.immu.compiler.element.predicate.ImmuPredicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
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

  public abstract List<ImmuPredicate.Result> validate(ProcessingEnvironment environment);

  public static <T extends ImmuElement> List<ImmuPredicate.Result> runPredicates(ProcessingEnvironment environment, T element, List<ImmuPredicate<T>> predicates) {
    return predicates
        .stream()
        .map((p) -> p.apply(environment, element))
        .collect(Collectors.toList());
  }

  protected final List<Element> methods() {
    return filter(ElementKind.METHOD);
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
