package immu;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import immu.classer.ImmuBuilderClasser;
import immu.classer.ImmuObjectClasser;
import immu.element.ImmuElement;
import immu.element.ImmuObjectElement;
import immu.element.predicate.ImmuPredicate;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Immu compiler.
 */
@AutoService(Processor.class)
public class ImmuCompiler implements Processor {

  private static final class ValidationResult {
    private final ImmuElement element;
    private final List<ImmuPredicate.Result> results;

    private ValidationResult(ImmuElement element, List<ImmuPredicate.Result> results) {
      this.element = element;
      this.results = results;
    }

    private static ValidationResult from(ProcessingEnvironment env, ImmuElement element) {
      return new ValidationResult(element, element.validate(env));
    }

    public boolean isSuccess() {
      return results
          .stream()
          .map(ImmuPredicate.Result::isSuccess)
          .allMatch((r) -> r);
    }
  }

  private ProcessingEnvironment env;

  @Override
  public Set<String> getSupportedOptions() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.of(Immu.class, SuperImmu.class)
        .map(Class::getName)
        .collect(Collectors.toSet());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
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

    final Set<ImmuObjectElement> validationElements = new HashSet<>();
    validationElements.addAll(objectElements);
    validationElements.addAll(superObjectElements);

    final List<ValidationResult> validationResults = validationElements
        .stream()
        .map((e) -> ValidationResult.from(env, e))
        .collect(Collectors.toList());

    validationResults.forEach((validation) -> {
      final List<String> warnings = new ArrayList<>();
      final List<String> errors = new ArrayList<>();

      validation.results.forEach((result) -> warnings.addAll(result.warnings));
      validation.results.forEach((result) -> errors.addAll(result.errors));

      if (!warnings.isEmpty()) {
        final String bigWarning = warnings
            .stream()
            .collect(Collectors.joining("\n"));

        env.getMessager().printMessage(Diagnostic.Kind.WARNING, bigWarning, validation.element.element());
      }

      if (!errors.isEmpty()) {
        final String bigError =errors
            .stream()
            .collect(Collectors.joining("\n"));

        env.getMessager().printMessage(Diagnostic.Kind.ERROR, bigError, validation.element.element());
      }
    });

    final boolean allValid = validationResults
        .stream()
        .map(ValidationResult::isSuccess)
        .allMatch((r) -> r);

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
        .forEach(this::writeSource);

    return true;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotationMirror, ExecutableElement executableElement, String s) {
    return Collections.emptyList();
  }

  void writeSource(JavaFile file) {
    try {
      file.writeTo(env.getFiler());
    } catch (IOException e) {
      env.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    }
  }
}
