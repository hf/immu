package immu;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests the generation of the builder class.
 */
public class ImmuBuilderGenerationTest {

  @Test
  public void generateBuilderForNoProperties() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("Empty",
            "import immu.Immu;",
            "@Immu",
            "public interface Empty {}"));

    assertMainOutline("Empty", compilation);
  }

  @Test
  public void generateBuilderForSingleProperty() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("SingleProperty",
            "import immu.Immu;",
            "@Immu",
            "public interface SingleProperty {",
            "int property();",
            "}"));

    assertMainOutline("SingleProperty", compilation);

    assertHasProperty("SingleProperty", compilation, "int property");
  }

  @Test
  public void generateBuilderForMultipleProperties() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("MultiProperty",
            "import immu.Immu;",
            "@Immu",
            "public interface MultiProperty {",
            "String propertyA();",
            "int propertyB();",
            "}"));

    assertMainOutline("MultiProperty", compilation);

    assertHasProperty("MultiProperty", compilation,
        "String propertyA",
        "int propertyB");
  }

  @Test
  public void generateBuilderFromNestedInterface() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("OuterClass",
            "import immu.Immu;",
            "public class OuterClass {",
            "@Immu",
            "public interface Empty {",
            "}",
            "}"));

    assertMainOutline("OuterClass.Empty", compilation);
  }

  private static void assertMainOutline(String immu, Compilation compilation) throws Exception {
    final String builder = immu.replaceAll("\\.", "") + "Builder";

    assertThat(compilation).generatedSourceFile(builder).contentsAsUtf8String().containsMatch("public\\s+final\\s+class\\s+" + builder + "\\s*\\{");
    assertThat(compilation).generatedSourceFile(builder).contentsAsUtf8String().containsMatch("private\\s+" + builder + "\\s*\\(\\s*\\)\\s*\\{");
    assertThat(compilation).generatedSourceFile(builder).contentsAsUtf8String().containsMatch("public\\s+static\\s+" + builder + "\\s+create\\s*\\([^)]*\\)\\s*\\{");
    assertThat(compilation).generatedSourceFile(builder).contentsAsUtf8String().containsMatch("public\\s+static\\s+" + builder + "\\s+from\\s*\\(" + immu + "\\s+immutable\\)\\s*\\{");
    assertThat(compilation).generatedSourceFile(builder).contentsAsUtf8String().containsMatch("public\\s+" + immu + "\\s+build\\(\\s*\\)\\s*\\{");
  }

  private static void assertHasProperty(String immu, Compilation compilation, String... properties) {
    final String builder = immu.replaceAll("\\.", "") + "Builder";

    Arrays.stream(properties)
        .map((p) -> p.split("\\s+"))
        .map((p) -> {
          p[0] = Pattern.quote(p[0]);
          return p;
        })
        .forEach((p) -> {
          assertThat(compilation).generatedSourceFile(builder).contentsAsUtf8String().containsMatch("public\\s+" + builder + "\\s+" + p[1] + "\\s*\\(\\s*(final\\s+)?" + p[0] + "\\s+" + p[1] + "\\s*\\)\\s*\\{");
        });
  }
}
