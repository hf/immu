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
            .addStatement("throw new $T($S)", ValueNotProvidedException.class, name)
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
      final Iterator<ImmuProperty> iterator = properties.iterator();

      final String firstName = iterator.next().name().toString();

      builder.addStatement("builder.append($S)", firstName + " = <");
      builder.addStatement("builder.append(this." + firstName + ")");

      while (iterator.hasNext()) {
        final String name = iterator.next().name().toString();

        builder.addStatement("builder.append($S)", "> , " + name + " = <");
        builder.addStatement("builder.append(this." + name + ")");
      }

      builder.addStatement("builder.append($S)", "> }");
    }

    return builder.addStatement("return builder.toString()")
        .build();
  }

  private CodeBlock hashCodeBlock(ClassName immuClass, List<ImmuProperty> properties) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    if (properties.isEmpty()) {
      builder.addStatement("return $T.class.hashCode()", immuClass);

      return builder.build();
    }

    builder.addStatement("int hashCode = $T.class.hashCode()", immuClass);

    for (ImmuProperty property : properties) {
      final String name = property.name().toString();
      final String hashCodeInvocation = hashCodeInvocation(name, property.returnType().getKind());

      if (property.isPrimitive() || property.isRequired()) {
        builder.addStatement("hashCode ^= " + hashCodeInvocation);
      } else {
        builder.addStatement("hashCode ^= ( null == this." + name + " ? 0 : " + hashCodeInvocation + " )");
      }
    }

    builder.addStatement("return hashCode");

    return builder.build();
  }

  private String hashCodeInvocation(String name, TypeKind kind) {
    final String value = "this." + name;

    switch (kind) {
      case ARRAY:
        return "java.util.Arrays.hashCode(" + value + ")";

      case INT:
        return value;

      case BYTE:
      case SHORT:
        return "((~0) & " + value + ")";

      case CHAR:
        return "((int) " + value + ")";

      case BOOLEAN:
        return "(" + value + "? 1 : 0)";

      case LONG:
        return "((int) (" + value + " >> 32) ^ (" + value + "))";

      case FLOAT:
        return "Float.floatToIntBits(" + value + ")";

      case DOUBLE:
        return "((int) (Double.doubleToLongBits(" + value + ") >> 32) ^ Double.doubleToLongBits(" + value + "))";

      default:
        return value + ".hashCode()";
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

    builder.addStatement("boolean equals = true");

    properties.forEach((p) -> {
      final String equalsInvocation = equalsInvocation(p);

      builder.addStatement("equals = equals && " + equalsInvocation);
    });

    builder.addStatement("return equals");

    return builder.build();
  }

  private String equalsInvocation(ImmuProperty property) {
    final String name = property.name().toString();
    final String a = "this." + name;
    final String b = "immuObject." + name + "()";

    final String invocation;

    switch (property.returnType().getKind()) {
      case DECLARED:
        invocation = a + ".equals(" + b + ")";
        break;

      case ARRAY:
        invocation = "java.util.Arrays.equals(" + a + ", " + b + ")";
        break;

      default:
        invocation = a + " == " + b;
        break;
    }

    if (!property.isRequired() && !property.isPrimitive()) {
      return "(" + a + " == " + b + ") || (null != " + a + " && null != " + b + " && " + a + ".equals(" + b + "))";
    }

    return "(" + invocation + ")";
  }
}
