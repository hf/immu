package immu.classer;

import com.squareup.javapoet.*;
import immu.element.ImmuObjectElement;
import immu.element.ImmuProperty;
import immu.ValueNotProvidedException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the class for the builder.
 *
 * @see #builderClass()
 */
public class ImmuBuilderClasser extends ImmuClasser {

  /**
   * Create a builder classer from the element.
   * @param element the element, must not be null
   * @return the classer, never null
   */
  public static ImmuBuilderClasser from(ImmuObjectElement element) {
    return new ImmuBuilderClasser(element);
  }

  ImmuBuilderClasser(ImmuObjectElement element) {
    super(element);
  }

  @Override
  public TypeSpec generate(ProcessingEnvironment env) {
    final ClassName immuClass = className();
    final ClassName objectClass = objectClass();
    final ClassName builderClass = builderClass();

    final Modifier[] modifiers;

    if (null != builderClass.enclosingClassName()) {
      modifiers = new Modifier[] { Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL };
    } else {
      modifiers = new Modifier[] { Modifier.PUBLIC, Modifier.FINAL };
    }

    final List<ImmuProperty> declaredProperties = element.properties();
    final List<ImmuProperty> inheritedProperties = element.superProperties(env);

    final List<ImmuProperty> properties = new ArrayList<>(declaredProperties.size() + inheritedProperties.size());

    properties.addAll(declaredProperties);
    properties.addAll(inheritedProperties);

    final List<FieldSpec> fields = properties
        .stream()
        .map((p) -> FieldSpec.builder(TypeName.get(p.returnType()), p.name().toString(), Modifier.TRANSIENT, Modifier.PRIVATE).build())
        .collect(Collectors.toList());

    final List<MethodSpec> setterMethods = properties
        .stream()
        .map((p) -> MethodSpec.methodBuilder(p.name().toString())
                      .addModifiers(Modifier.PUBLIC)
                      .addParameter(TypeName.get(p.returnType()), p.name().toString(), Modifier.FINAL)
                      .returns(builderClass)
                      .addCode(propertySetter(p))
                      .build())
        .collect(Collectors.toList());

    final StringBuilder builder = new StringBuilder();
    final Iterator<ImmuProperty> iterator = properties.iterator();

    if (iterator.hasNext()) {
      builder.append(iterator.next().name().toString());
    }

    while (iterator.hasNext()) {
      builder.append(", ");
      builder.append(iterator.next().name().toString());
    }

    final String statementList = builder.toString();

    final MethodSpec buildMethod = MethodSpec.methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(immuClass)
        .addCode(CodeBlock.builder()
            .addStatement("return new $T(" + statementList + ")", objectClass)
            .build())
        .build();

    final CodeBlock.Builder copierBlockBuilder = CodeBlock.builder();

    copierBlockBuilder.addStatement("final $T builder = new $T()", builderClass, builderClass);

    properties
        .stream()
        .map((p) -> p.name().toString())
        .forEach((n) -> copierBlockBuilder.addStatement("builder." + n + " = " + "immutable." + n + "()"));

    copierBlockBuilder.addStatement("return builder");

    final MethodSpec copierStatic = MethodSpec.methodBuilder("from")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(builderClass)
        .addParameter(immuClass, "immutable")
        .addCode(copierBlockBuilder.build())
        .build();

    final List<ImmuProperty> requiredProperties = properties
        .stream()
        .filter(ImmuProperty::isRequired)
        .collect(Collectors.toList());

    final MethodSpec creatorStatic = MethodSpec.methodBuilder("create")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameters(requiredProperties.stream()
          .map((p) -> ParameterSpec.builder(TypeName.get(p.returnType()), p.name().toString()).build())
          .collect(Collectors.toList()))
        .addCode(creatorCodeBlock(builderClass, requiredProperties))
        .returns(builderClass)
        .build();

    final MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .build();

    return TypeSpec.classBuilder(builderClass)
        .addModifiers(modifiers)
        .addFields(fields)
        .addMethod(creatorStatic)
        .addMethod(copierStatic)
        .addMethod(constructor)
        .addMethods(setterMethods)
        .addMethod(buildMethod)
        .build();
  }

  private CodeBlock propertySetter(ImmuProperty property) {
    final String name = property.name().toString();

    final CodeBlock.Builder builder = CodeBlock.builder();

    if (property.isRequired()) {
      builder.beginControlFlow("if (null == " + name + ")");
      builder.addStatement("throw new $T($S)", ValueNotProvidedException.class, name);
      builder.endControlFlow();
    }

    builder.addStatement("this." + name + " = " + name);
    builder.addStatement("return this");

    return builder.build();
  }

  private CodeBlock creatorCodeBlock(ClassName builderClass, List<ImmuProperty> properties) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    if (properties.isEmpty()) {
      builder.addStatement("return new $T()", builderClass);
      return builder.build();
    }

    properties
        .stream()
        .filter((p) -> !p.isPrimitive())
        .forEach((p) -> {
          final String name = p.name().toString();

          builder.beginControlFlow("if (null == " + name + ")");
          builder.addStatement("throw new $T($S)", ValueNotProvidedException.class, name);
          builder.endControlFlow();
        });

    builder.addStatement("final $T builder = new $T()", builderClass, builderClass);

    properties.forEach((p) -> {
      final String name = p.name().toString();

      builder.addStatement("builder." + name + " = " + name);
    });

    builder.addStatement("return builder");

    return builder.build();
  }
}
