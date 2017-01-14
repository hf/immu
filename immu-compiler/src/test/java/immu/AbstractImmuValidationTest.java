package immu;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests compiler validations for a given annotation name.
 */
public abstract class AbstractImmuValidationTest {

  private final String annotation;

  /**
   * Construct an abstract test.
   * @param annotation the annotation name, should be just the simple name, must not be null
   */
  protected AbstractImmuValidationTest(String annotation) {
    this.annotation = annotation;
  }

  @Test
  public void notAllowedForClass() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("NonInterface",
            "import immu." + annotation + ";",
            "@" + annotation,
            "public class NonInterface {}"));

    assertThat(compilation).hadErrorContainingMatch("NonInterface is not an interface");
  }

  @Test
  public void notAllowedForAbstractClass() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("NonInterface",
            "import immu." + annotation + ";",
            "@" + annotation,
            "public abstract class NonInterface {}"));

    assertThat(compilation).hadErrorContainingMatch("NonInterface is not an interface");
  }

  @Test
  public void notAllowedForAnnotation() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("NonInterface",
            "import immu." + annotation + ";",
            "@" + annotation,
            "public @interface NonInterface {}"));

    assertThat(compilation).hadErrorContainingMatch("NonInterface is not an interface");
  }

  @Test
  public void nonAnnotatedImmediateSuperInterfaceMustBeEmpty() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(
            JavaFileObjects.forSourceLines("SuperInterface",
                "public interface SuperInterface {",
                "String FIELD = \"field\";",
                "void methodA();",
                "String methodB();",
                "}"),
            JavaFileObjects.forSourceLines("InvalidInterface",
            "import immu." + annotation + ";",
            "@" + annotation,
            "public interface InvalidInterface extends SuperInterface {}"));

    assertThat(compilation).hadErrorContainingMatch("InvalidInterface extends SuperInterface");
    assertThat(compilation).hadErrorContainingMatch("SuperInterface#methodA");
    assertThat(compilation).hadErrorContainingMatch("SuperInterface#methodB");
  }

  @Test
  public void nonAnnotatedTransitiveSuperInterfaceMustBeEmpty() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(
            JavaFileObjects.forSourceLines("TransitiveInterface",
                "public interface TransitiveInterface {",
                "void transitiveA();",
                "int transitiveB();",
                "}"),
            JavaFileObjects.forSourceLines("SuperInterface",
                "public interface SuperInterface extends TransitiveInterface {",
                "void methodA();",
                "String methodB();",
                "}"),
            JavaFileObjects.forSourceLines("InvalidInterface",
                "import immu." + annotation + ";",
                "@" + annotation,
                "public interface InvalidInterface extends SuperInterface {}"));

    assertThat(compilation).hadErrorContainingMatch("InvalidInterface extends SuperInterface");
    assertThat(compilation).hadErrorContainingMatch("SuperInterface extends TransitiveInterface");
    assertThat(compilation).hadErrorContainingMatch("SuperInterface#methodA");
    assertThat(compilation).hadErrorContainingMatch("SuperInterface#methodB");
    assertThat(compilation).hadErrorContainingMatch("TransitiveInterface#transitiveA");
    assertThat(compilation).hadErrorContainingMatch("TransitiveInterface#transitiveB");
  }

  @Test
  public void notAllowedPropertiesWithParameters() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("InvalidInterface",
            "import immu." + annotation + ";",
            "@" + annotation,
            "public interface InvalidInterface {",
            "long property();",
            "String propertyA(int param);",
            "int propertyB(int param);",
            "}"));

    assertThat(compilation).hadErrorContainingMatch("has method propertyA with parameters");
    assertThat(compilation).hadErrorContainingMatch("has method propertyB with parameters");
  }

  @Test
  public void notAllowedPropertiesThatReturnVoid() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("InvalidInterface",
            "import immu." + annotation + ";",
            "@" + annotation,
            "public interface InvalidInterface {",
            "long property();",
            "void propertyA();",
            "void propertyB();",
            "}"));

    assertThat(compilation).hadErrorContainingMatch("method propertyA .* returns void");
    assertThat(compilation).hadErrorContainingMatch("method propertyB .* returns void");
  }

  @Test
  public void notAllowedPropertiesThatThrow() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("InvalidInterface",
            "import immu." + annotation + ";",
            "import java.io.IOException;",
            "@" + annotation,
            "public interface InvalidInterface {",
            "long property();",
            "String propertyA() throws IOException;",
            "int propertyB() throws IOException;",
            "}"));

    assertThat(compilation).hadErrorContainingMatch("method propertyA .* throws");
    assertThat(compilation).hadErrorContainingMatch("method propertyB .* throws");
  }

  @Test
  public void notAllowedProperiesWithTypeVariables() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("InvalidInterface",
            "import immu." + annotation + ";",
            "@" + annotation,
            "public interface InvalidInterface {",
            "long property();",
            "<T> T propertyA(Class<T> tClass);",
            "<C> C propertyB();",
            "}"));

    assertThat(compilation).hadErrorContainingMatch("method propertyA .* type variables");
    assertThat(compilation).hadErrorContainingMatch("method propertyB .* type variables");
  }

  @Test
  public void transitiveInheritanceOfImmus() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(
            JavaFileObjects.forSourceLines("SuperInterface",
                "import immu." + annotation + ";",
                "@" + annotation,
                "public interface SuperInterface {",
                "String superProperty();",
                "}"),
            JavaFileObjects.forSourceLines("Interface",
                "import immu." + annotation + ";",
                "@" + annotation,
                "public interface Interface extends SuperInterface {",
                "int property();",
                "}"));

    assertThat(compilation).hadErrorCount(0);
  }

  @Test
  public void warnIfHasBothImmuAndSuperImmu() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(
            JavaFileObjects.forSourceLines("Both",
                "import immu.Immu;",
                "import immu.SuperImmu;",
                "@Immu",
                "@SuperImmu",
                "public interface Both { }"));

    assertThat(compilation).hadWarningContainingMatch("Both.+annotated.+@Immu.+@SuperImmu");
  }

  @Test
  public void exerciseSupperImmuWithMoreThanMethods() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(
            JavaFileObjects.forSourceLines("Both",
                "import immu.Immu;",
                "import immu.SuperImmu;",
                "public class Both {",
                "@SuperImmu",
                "public interface Upper {",
                "String FIELD = \"field\";",
                "int upperProperty();",
                "}",
                "@Immu",
                "public interface Lower extends Upper {",
                "int property();",
                "}",
                "}"));

    assertThat(compilation).hadErrorCount(0);
  }

}
