package me.stojan.immu.compiler.classer;

import com.squareup.javapoet.*;
import me.stojan.immu.compiler.element.ImmuObjectElement;
import me.stojan.immu.compiler.element.ImmuProperty;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.util.*;
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

    final Modifier[] modifiers;

    if (null != objectClass.enclosingClassName()) {
      modifiers = new Modifier[] { Modifier.STATIC, Modifier.FINAL };
    } else {
      modifiers = new Modifier[] { Modifier.FINAL };
    }

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

    final CodeBlock constructorNonNullChecker = properties
        .stream()
        .filter(ImmuProperty::isNonNull)
        .map((p) -> p.name().toString())
        .reduce(CodeBlock.builder(), (cb, s) -> cb.beginControlFlow("if (null == " + s + ")").addStatement("throw new $T($S)", IllegalArgumentException.class, "Argument" + s + " must not be null").endControlFlow(), (cba, cbb) -> cba)
        .build();

    final CodeBlock constructorInitializer = properties
        .stream()
        .map((p) -> p.name().toString())
        .map((p) -> "this." + p + " = " + p)
        .reduce(CodeBlock.builder(), (cb, s) -> cb.addStatement(s), (cba, cbb) -> cba)
        .build();

    final MethodSpec constructor = MethodSpec.constructorBuilder()
        .addParameters(parameters)
        .addCode(constructorNonNullChecker)
        .addCode(constructorInitializer)
        .build();

    final MethodSpec hashCode = MethodSpec.methodBuilder("hashCode")
        .addModifiers(Modifier.PUBLIC)
        .returns(int.class)
        .addAnnotation(Override.class)
        .addCode(hashCodeBlock(properties))
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

    return TypeSpec.classBuilder(objectClass)
        .addModifiers(modifiers)
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

  private CodeBlock hashCodeBlock(List<ImmuProperty> properties) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    if (properties.isEmpty()) {
      builder.addStatement("return super.hashCode()");

      return builder.build();
    }

    builder.addStatement("int hashCode = 0");

    for (ImmuProperty property : properties) {
      final String name = property.name().toString();
      final String hashCodeInvocation = hashCodeInvocation(name, property.returnType().getKind());

      if (property.isPrimitive() || property.isNonNull()) {
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

      case LONG:
        return "((int) (" + value + " >> 32) ^ (" + value + "))";

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

    if (!property.isNonNull() && !property.isPrimitive()) {
      return "(" + a + " == " + b + ") || (null != " + a + " && null != " + b + " && " + a + ".equals(" + b + "))";
    }

    return "(" + invocation + ")";
  }
}
