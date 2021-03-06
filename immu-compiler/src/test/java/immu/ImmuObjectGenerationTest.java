package immu;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests the generation of the immutable object.
 */
public class ImmuObjectGenerationTest {

  @Test
  public void generateImmutableObjectForNoProperties() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("Empty",
            "import immu.Immu;",
            "@Immu",
            "public interface Empty {}"));

    assertMainOutline("Empty", compilation);
  }

  @Test
  public void generateImmutableObjectWithOneProperty() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("SingleProperty",
            "import immu.Immu;",
            "@Immu",
            "public interface SingleProperty {",
            "int property();",
            "}"));

    assertMainOutline("SingleProperty", compilation);
    assertHasProperties("SingleProperty", compilation, "int property");
  }

  @Test
  public void generateImmutableObjectWithRequiredObjectProperty() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("SingleProperty",
            "import immu.Immu;",
            "import immu.Required;",
            "@Immu",
            "public interface SingleProperty {",
            "@Required",
            "String property();",
            "}"));

    assertMainOutline("SingleProperty", compilation);
  }

  @Test
  public void generateImmutableObjectWithRequiredArrayProperty() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("SingleProperty",
            "import immu.Immu;",
            "import immu.Required;",
            "@Immu",
            "public interface SingleProperty {",
            "@Required",
            "int[] property();",
            "}"));

    assertMainOutline("SingleProperty", compilation);
  }

  @Test
  public void generateImmutableObjectWithMultipleRequiredProperties() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("MultiProperty",
            "import immu.Immu;",
            "import immu.Required;",
            "@Immu",
            "public interface MultiProperty {",
            "@Required int propertyInt();",
            "@Required boolean propertyBoolean();",
            "@Required byte propertyByte();",
            "@Required short propertyShort();",
            "@Required char propertyChar();",
            "@Required long propertyLong();",
            "@Required float propertyFloat();",
            "@Required double propertyDouble();",
            "@Required int[] propertyIntArray();",
            "@Required String propertyString();",
            "}"));

    assertMainOutline("MultiProperty", compilation);
    assertHasProperties("MultiProperty", compilation,
        "int propertyInt",
        "byte propertyByte",
        "boolean propertyBoolean",
        "short propertyShort",
        "long propertyLong",
        "float propertyFloat",
        "char propertyChar",
        "double propertyDouble",
        "int[] propertyIntArray",
        "String propertyString");
  }

  @Test
  public void generateImmutableObjectWithMultipleProperties() throws Exception {
    Compilation compilation = javac()
        .withProcessors(new ImmuCompiler())
        .compile(JavaFileObjects.forSourceLines("MultiProperty",
            "import immu.Immu;",
            "@Immu",
            "public interface MultiProperty {",
            "int propertyInt();",
            "boolean propertyBoolean();",
            "byte propertyByte();",
            "short propertyShort();",
            "char propertyChar();",
            "long propertyLong();",
            "float propertyFloat();",
            "double propertyDouble();",
            "int[] propertyIntArray();",
            "String propertyString();",
            "}"));

    assertMainOutline("MultiProperty", compilation);
    assertHasProperties("MultiProperty", compilation,
        "int propertyInt",
        "byte propertyByte",
        "boolean propertyBoolean",
        "short propertyShort",
        "long propertyLong",
        "float propertyFloat",
        "char propertyChar",
        "double propertyDouble",
        "int[] propertyIntArray",
        "String propertyString");
  }

  @Test
  public void generateImmutableObjectFromNestedInterface() throws Exception {
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
    final String object = "Immutable" + immu.replaceAll("\\.", "");

    assertThat(compilation).generatedSourceFile(object).contentsAsUtf8String().containsMatch("(?!public)\\s+final\\s+class\\s+" + object + "\\s+implements\\s+" + immu + "\\s*,\\s*Immutable\\s*\\{");
    assertThat(compilation).generatedSourceFile(object).contentsAsUtf8String().containsMatch("@Override\\s+public\\s+boolean\\s+equals\\s*\\(\\s*(final\\s+)?Object\\s+[a-z]+\\s*\\)\\s*\\{");
    assertThat(compilation).generatedSourceFile(object).contentsAsUtf8String().containsMatch("@Override\\s+public\\s+int\\s+hashCode\\s*\\(\\s*\\)\\s*\\{");
    assertThat(compilation).generatedSourceFile(object).contentsAsUtf8String().containsMatch("@Override\\s+public\\s+String\\s+toString\\s*\\(\\s*\\)\\s*\\{");
  }

  private static void assertHasProperties(String immu, Compilation compilation, String... properties) throws Exception {
    final String object = "Immutable" + immu;

    Arrays.stream(properties)
        .map((p) -> p.split("\\s+"))
        .forEach((p) -> assertThat(compilation).generatedSourceFile(object).contentsAsUtf8String().containsMatch("@Override\\s+public\\s+" + Pattern.quote(p[0]) + "\\s+" + p[1] + "\\s*\\(\\s*\\)\\s*\\{\\s*return\\s+(this\\s*\\.\\s*)?" + p[1] + "\\s*;\\s*\\}"));
  }
}
