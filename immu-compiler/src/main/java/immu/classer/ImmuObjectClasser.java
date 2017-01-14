package immu.classer;

import com.squareup.javapoet.*;
import immu.ValueNotProvidedException;
import immu.element.ImmuObjectElement;
import immu.element.ImmuProperty;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Generates the class for the immutable object implementation.
 *
 * @see #objectClass()
 */
public class ImmuObjectClasser extends ImmuClasser {

  /**
   * Create an object classer from the element.
   * @param element the element, must not be null
   * @return the object classer, never null
   */
  public static ImmuObjectClasser from(ImmuObjectElement element) {
    return new ImmuObjectClasser(element);
  }

  ImmuObjectClasser(ImmuObjectElement element) {
    super(element);
  }

  @Override
  public TypeSpec generate(ProcessingEnvironment env) {
    final ClassName immuClass = className();
    final ClassName objectClass = objectClass();

    final List<ImmuProperty> declaredProperties = element.properties();
    final List<ImmuProperty> inheritedProperties = element.superProperties(env);

    final List<ImmuProperty> properties = new ArrayList<>(declaredProperties.size() + inheritedProperties.size());

    properties.addAll(declaredProperties);
    properties.addAll(inheritedProperties);

    final List<FieldSpec> fields = properties
        .stream()
        .map((p) -> FieldSpec.builder(TypeName.get(p.returnType()), p.name().toString(), Modifier.PRIVATE, Modifier.FINAL).build())
        .collect(Collectors.toList());

    final List<ParameterSpec> parameters = properties
        .stream()
        .map((p) -> ParameterSpec.builder(TypeName.get(p.returnType()), p.name().toString()).build())
        .collect(Collectors.toList());

    final CodeBlock constructorDeclaredRequiredChecker = properties
        .stream()
        .filter((p) -> !p.isPrimitive())
        .filter(ImmuProperty::isRequired)
        .map((p) -> p.name().toString())
        .reduce(CodeBlock.builder(), (cb, name) -> cb
            .beginControlFlow("if (null == " + name + ")")
            .addStatement("throw $T.forProperty($S)", ValueNotProvidedException.class, name)
            .endControlFlow(), (cba, cbb) -> cba)
        .build();

    final CodeBlock constructorInitializer = properties
        .stream()
        .map((p) -> p.name().toString())
        .map((p) -> "this." + p + " = " + p)
        .reduce(CodeBlock.builder(), (cb, s) -> cb.addStatement(s), (cba, cbb) -> cba)
        .build();

    final MethodSpec constructor = MethodSpec.constructorBuilder()
        .addParameters(parameters)
        .addCode(constructorDeclaredRequiredChecker)
        .addCode(constructorInitializer)
        .build();

    final MethodSpec hashCode = MethodSpec.methodBuilder("hashCode")
        .addModifiers(Modifier.PUBLIC)
        .returns(int.class)
        .addAnnotation(Override.class)
        .addCode(hashCodeBlock(immuClass, properties))
        .build();

    final MethodSpec toString = MethodSpec.methodBuilder("toString")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addAnnotation(Override.class)
        .addCode(toStringBlock(immuClass, properties))
        .build();

    final MethodSpec equals = MethodSpec.methodBuilder("equals")
        .addModifiers(Modifier.PUBLIC)
        .returns(boolean.class)
        .addAnnotation(Override.class)
        .addParameter(Object.class, "object")
        .addCode(equalsBlock(immuClass, properties))
        .build();

    final List<MethodSpec> methods = properties
        .stream()
        .map((p) -> MethodSpec.methodBuilder(p.name().toString())
                      .addModifiers(Modifier.PUBLIC)
                      .returns(TypeName.get(p.returnType()))
                      .addAnnotation(Override.class)
                      .addCode(CodeBlock.builder()
                          .addStatement("return this." + p.name().toString())
                          .build())
                      .build())
        .collect(Collectors.toList());

    final List<TypeVariableName> typeVariables = element.typeElement().getTypeParameters()
        .stream()
        .map(TypeVariableName::get)
        .collect(Collectors.toList());

    return TypeSpec.classBuilder(objectClass)
        .addModifiers(Modifier.FINAL)
        .addTypeVariables(typeVariables)
        .addSuperinterface(immuClass)
        .addFields(fields)
        .addMethod(constructor)
        .addMethods(methods)
        .addMethod(hashCode)
        .addMethod(equals)
        .addMethod(toString)
        .build();
  }

  private CodeBlock toStringBlock(ClassName immuClass, List<ImmuProperty> properties) {
    final CodeBlock.Builder builder = CodeBlock.builder()
          .addStatement("final $T builder = new $T()", StringBuilder.class, StringBuilder.class)
          .addStatement("builder.append(\"$T@\")", immuClass)
          .addStatement("builder.append(String.format(($T) null, $S, $T.identityHashCode(this)))", Locale.class, "@%08x", System.class);

    if (properties.isEmpty()) {
      builder.addStatement("builder.append($S)", "{  }");
    } else {
      builder.addStatement("builder.append($S)", "{ ");
      toStringInvocation(builder, properties.get(0));

      properties
          .stream()
          .skip(1)
          .forEach((p) -> {
            builder.addStatement("builder.append($S)", ", ");
            toStringInvocation(builder, p);
          });

      builder.addStatement("builder.append($S)", " }");
    }

    return builder.addStatement("return builder.toString()")
        .build();
  }

  private void toStringInvocation(CodeBlock.Builder builder, ImmuProperty p) {
    final String name = p.name().toString();

    builder.addStatement("builder.append($S)", name + " = ");

    if (p.isRequired() || p.isPrimitive()) {
      builder.addStatement("builder.append('<')");
      builder.addStatement("builder.append(this." + name + ")");
      builder.addStatement("builder.append('>')");
    } else {
      builder.beginControlFlow("if (null != this." + name + ")");
      builder.addStatement("builder.append('<')");
      builder.addStatement("builder.append(this." + name + ")");
      builder.addStatement("builder.append('>')");
      builder.nextControlFlow("else");
      builder.addStatement("builder.append($S)", "@null");
      builder.endControlFlow();
    }
  }

  private CodeBlock hashCodeBlock(ClassName immuClass, List<ImmuProperty> properties) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    if (properties.isEmpty()) {
      builder.addStatement("return $T.class.getCanonicalName().hashCode()", immuClass);

      return builder.build();
    }

    builder.addStatement("int hashCode = $T.class.getCanonicalName().hashCode()", immuClass);

    for (ImmuProperty property : properties) {
      builder.addStatement("hashCode ^= " + hashCodeInvocation(property));
    }

    builder.addStatement("return hashCode");

    return builder.build();
  }

  private String hashCodeInvocation(ImmuProperty property) {
    final String value = "this." + property.name();
    final TypeKind kind = property.returnType().getKind();

    final String invocation;

    switch (kind) {
      case ARRAY:
        return "java.util.Arrays.hashCode(" + value + ")";

      case INT:
      case CHAR:
      case BYTE:
      case SHORT:
        return value;

      case BOOLEAN:
        return "(" + value + "? 1 : 0)";

      case LONG:
        return "(int) ((" + value + " >> 32) ^ (" + value + "))";

      case FLOAT:
        return "Float.floatToIntBits(" + value + ")";

      case DOUBLE:
        return "(int) ((Double.doubleToLongBits(" + value + ") >> 32) ^ Double.doubleToLongBits(" + value + "))";

      default:
        if (property.isRequired()) {
          return value + ".hashCode()";
        }

        return "java.util.Objects.hashCode(" + value + ")";
    }
  }

  private CodeBlock equalsBlock(ClassName immuClass, List<ImmuProperty> properties) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    builder.beginControlFlow("if (this == object)");
    builder.addStatement("return true");
    builder.endControlFlow();

    builder.beginControlFlow("if (null == object)");
    builder.addStatement("return false");
    builder.endControlFlow();

    builder.beginControlFlow("if (!(object instanceof $T))", immuClass);
    builder.addStatement("return false");
    builder.endControlFlow();

    builder.addStatement("final $T immuObject = ($T) object", immuClass, immuClass);

    properties.forEach((p) -> {
      final String equalsInvocation = notEqualsInvocation(p);

      builder.beginControlFlow("if (" + equalsInvocation + ")");
      builder.addStatement("return false");
      builder.endControlFlow();
    });

    builder.addStatement("return true");

    return builder.build();
  }

  private String notEqualsInvocation(ImmuProperty property) {
    final String name = property.name().toString();
    final String a = "this." + name;
    final String b = "immuObject." + name + "()";

    switch (property.returnType().getKind()) {
      case DECLARED:
        if (property.isRequired()) {
          return "!" + a + ".equals(" + b + ")";
        }

        return "!java.util.Objects.equals(" + a + ", " + b + ")";

      case ARRAY:
        return "!java.util.Arrays.equals(" + a + ", " + b + ")";

      default:
        return a + " != " + b;
    }
  }
}
