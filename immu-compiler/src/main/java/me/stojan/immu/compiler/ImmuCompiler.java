package me.stojan.immu.compiler;

import com.squareup.javapoet.JavaFile;
import me.stojan.immu.annotation.Immu;
import me.stojan.immu.annotation.SuperImmu;
import me.stojan.immu.compiler.element.ImmuObjectElement;
import me.stojan.immu.compiler.classer.ImmuBuilderClasser;
import me.stojan.immu.compiler.classer.ImmuObjectClasser;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vuk on 05/01/17.
 */
public class ImmuCompiler implements Processor {

  private ProcessingEnvironment env;

  @Override
  public Set<String> getSupportedOptions() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(Immu.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_7;
  }

  @Override
  public void init(ProcessingEnvironment processingEnvironment) {
    env = processingEnvironment;
  }

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    final Set<? extends Element> immuElements = roundEnvironment.getElementsAnnotatedWith(Immu.class);
    final Set<? extends Element> superImmuElements = roundEnvironment.getElementsAnnotatedWith(SuperImmu.class);

    final List<ImmuObjectElement> objectElements = immuElements
        .stream()
        .map(ImmuObjectElement::from)
        .collect(Collectors.toList());

    final List<ImmuObjectElement> superObjectElements = superImmuElements
        .stream()
        .map(ImmuObjectElement::from)
        .collect(Collectors.toList());

    final boolean allValid = Stream.concat(superObjectElements.stream(), objectElements.stream())
        .map((e) -> e.validate(env))
        .reduce(true, (a, b) -> a && b);

    if (!allValid) {
      return false;
    }

    final Stream<JavaFile> objectClassers = objectElements
        .stream()
        .map(ImmuObjectClasser::from)
        .map((c) -> JavaFile.builder(c.className().packageName(), c.generate(env)).build());

    final Stream<JavaFile> builderClassers = objectElements
        .stream()
        .map(ImmuBuilderClasser::from)
        .map((c) -> JavaFile.builder(c.className().packageName(), c.generate(env)).build());

    Stream.concat(objectClassers, builderClassers)
        .collect(Collectors.toList())
        .forEach((file) -> {
          try {
            file.writeTo(env.getFiler());
          } catch (IOException e) {
            env.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
          }
        });

    return true;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotationMirror, ExecutableElement executableElement, String s) {
    return Collections.emptyList();
  }
}
