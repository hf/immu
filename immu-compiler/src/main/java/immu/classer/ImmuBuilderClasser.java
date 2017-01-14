package immu.classer;

import com.squareup.javapoet.*;
import immu.ValueNotProvidedException;
import immu.element.ImmuElement;
import immu.element.ImmuObjectElement;
import immu.element.ImmuProperty;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generates the class for the builder.
 *
 * @see #builderClass()
 */
public class ImmuBuilderClasser extends ImmuClasser {

  private static final class PropertyWithIndex {
    private int index;
    private ImmuProperty property;

    private PropertyWithIndex(int index) {
      this.index = index;
    }

    private PropertyWithIndex update(List<ImmuProperty> properties) {
      this.property = properties.get(index);

      return this;
    }
  }

  private static final class AnalyzedProperties {
    private final List<ImmuProperty> properties;
    private final List<ImmuProperty> requiredProperties;
    private final List<ImmuProperty> nonIndexedProperties;
    private final List<PropertyWithIndex> indexedProperties;

    private final int checkerInts;

    private AnalyzedProperties(List<ImmuProperty> properties, List<ImmuProperty> requiredProperties, List<ImmuProperty> nonIndexedProperties, List<PropertyWithIndex> indexedProperties, int checkerInts) {
      this.properties = properties;
      this.requiredProperties = requiredProperties;
      this.nonIndexedProperties = nonIndexedProperties;
      this.indexedProperties = indexedProperties;
      this.checkerInts = checkerInts;
    }
  }

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
    final ClassName builderClass = builderClass();

    final List<ImmuProperty> declaredProperties = element.properties();
    final List<ImmuProperty> inheritedProperties = element.superProperties(env);

    final List<ImmuProperty> properties = new ArrayList<>(declaredProperties.size() + inheritedProperties.size());

    properties.addAll(declaredProperties);
    properties.addAll(inheritedProperties);

    final AnalyzedProperties analyzedProperties = analyzeProperties(properties);

    return TypeSpec.classBuilder(builderClass)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addTypeVariables(typeVariables())
        .addFields(fields(analyzedProperties))
        .addMethods(creators(analyzedProperties))
        .addMethod(copierStatic(analyzedProperties))
        .addMethod(constructor(analyzedProperties))
        .addMethods(setters(analyzedProperties))
        .addMethod(build(analyzedProperties))
        .build();
  }

  private static AnalyzedProperties analyzeProperties(List<ImmuProperty> properties) {
    final List<ImmuProperty> requiredProperties = properties
        .stream()
        .filter(ImmuProperty::isRequired)
        .collect(Collectors.toList());

    final List<ImmuProperty> requiredPrimitiveProperties = requiredProperties
        .stream()
        .filter(ImmuProperty::isPrimitive)
        .collect(Collectors.toList());

    final List<ImmuProperty> nonIndexedProperties = properties
        .stream()
        .filter((p) -> !p.isPrimitive() || !p.isRequired())
        .collect(Collectors.toList());

    final List<PropertyWithIndex> indexedProperties = IntStream.range(0, requiredPrimitiveProperties.size())
        .mapToObj(PropertyWithIndex::new)
        .map((pi) -> pi.update(requiredPrimitiveProperties))
        .collect(Collectors.toList());

    final int reqSize = requiredPrimitiveProperties.size();
    final int checkerInts = reqSize / 32 + ((reqSize % 32) > 0 ? 1 : 0);

    return new AnalyzedProperties(properties, requiredProperties, nonIndexedProperties, indexedProperties, checkerInts);
  }

  private Iterable<TypeVariableName> typeVariables() {
    return element.typeElement().getTypeParameters()
        .stream()
        .map(TypeVariableName::get)
        .collect(Collectors.toList());
  }

  private Iterable<FieldSpec> fields(AnalyzedProperties analyzedProperties) {
    final List<FieldSpec> propertyFields = analyzedProperties.properties
        .stream()
        .map((p) -> FieldSpec.builder(TypeName.get(p.returnType()), p.name().toString(), Modifier.TRANSIENT, Modifier.PRIVATE).build())
        .collect(Collectors.toList());

    if (analyzedProperties.checkerInts > 0) {
      final FieldSpec checkedField = FieldSpec.builder(TypeName.get(int[].class), "checked", Modifier.PRIVATE, Modifier.FINAL, Modifier.TRANSIENT)
          .initializer("new int[" + analyzedProperties.checkerInts + "]")
          .build();

      return Stream.concat(Stream.of(checkedField), propertyFields.stream()).collect(Collectors.toList());
    }

    return propertyFields;
  }

  private Iterable<MethodSpec> setters(AnalyzedProperties analyzedProperties) {
    final Stream<MethodSpec.Builder> regular = analyzedProperties.nonIndexedProperties
        .stream()
        .map((p) -> propertySetter(p).addCode(propertySetterCodeBlock(p)));

    final Stream<MethodSpec.Builder> indexed = analyzedProperties.indexedProperties
        .stream()
        .map((pi) -> propertySetter(pi.property).addCode(propertySetterCodeBlock(pi)));

    return Stream.concat(regular, indexed)
        .map(MethodSpec.Builder::build)
        .collect(Collectors.toList());
  }

  private MethodSpec.Builder propertySetter(ImmuProperty property) {
    final String name = property.name().toString();
    final ClassName builderClass = builderClass();

    return MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(property.returnType()), name, Modifier.FINAL)
            .returns(builderClass);
  }

  private CodeBlock propertySetterCodeBlock(ImmuProperty property) {
    final String name = property.name().toString();

    final CodeBlock.Builder builder = CodeBlock.builder();

    builder.addStatement("this." + name + " = " + name);
    builder.addStatement("return this");

    return builder.build();
  }

  private CodeBlock propertySetterCodeBlock(PropertyWithIndex propertyWithIndex) {
    final String name = propertyWithIndex.property.name().toString();

    final CodeBlock.Builder builder = CodeBlock.builder();

    final int propertyIntIndex = propertyWithIndex.index / 32;
    final int propertyBitIndex = propertyWithIndex.index % 32;

    builder.addStatement("this." + name + " = " + name);
    builder.addStatement("this.checked[" + propertyIntIndex + "] |= 1 << " + propertyBitIndex);

    builder.addStatement("return this");

    return builder.build();
  }

  private MethodSpec constructor(AnalyzedProperties analyzedProperties) {
    final MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE);

    if (analyzedProperties.checkerInts > 0) {
      final int unusedBitsN = analyzedProperties.indexedProperties.size() % 32;

      if (unusedBitsN > 0) {
        final int unusedBits = (~0) << unusedBitsN;

        builder.addStatement("this.checked[this.checked.length - 1] = 0x" + Integer.toHexString(unusedBits));
      }
    }

    return builder.build();
  }

  private Iterable<MethodSpec> creators(AnalyzedProperties analyzedProperties) {
    return Collections.singletonList(creatorStatic());
  }

  private MethodSpec creatorStatic() {
    final ClassName builderClass = builderClass();

    return MethodSpec.methodBuilder("create")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addCode(creatorCodeBlock(builderClass))
        .returns(builderClass)
        .build();
  }

  private MethodSpec copierStatic(AnalyzedProperties analyzedProperties) {
    final ClassName builderClass = builderClass();
    final ClassName immuClass = className();

    return MethodSpec.methodBuilder("from")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(builderClass)
        .addParameter(immuClass, "immutable")
        .addCode(copierCodeBlock(builderClass, analyzedProperties.properties))
        .build();
  }

  private MethodSpec build(AnalyzedProperties analyzedProperties) {
    final ClassName immuClass = className();
    final ClassName objectClass = objectClass();

    final String statementList = analyzedProperties.properties
        .stream()
        .map(ImmuElement::name)
        .map(Name::toString)
        .collect(Collectors.joining(", "));

    final MethodSpec.Builder builder = MethodSpec.methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(immuClass);

    final List<PropertyWithIndex> indexedProperties = analyzedProperties.indexedProperties;

    for (int i = 0; i < analyzedProperties.checkerInts; i++) {
      final CodeBlock.Builder checkerBuilder = CodeBlock.builder();

      checkerBuilder.beginControlFlow("if ((~0) != this.checked[" + i + "])");

      if (indexedProperties.size() > 1) {
        final String props = indexedProperties.subList(32 * i, Math.min(32 * (i + 1), indexedProperties.size()))
            .stream()
            .map((p) -> p.property)
            .map(ImmuProperty::name)
            .map(Name::toString)
            .collect(Collectors.joining(", "));

        checkerBuilder.addStatement("throw $T.forProperties($S)", ValueNotProvidedException.class, props);
      } else {
        final String prop = indexedProperties.get(0).property.name().toString();

        checkerBuilder.addStatement("throw $T.forProperty($S)", ValueNotProvidedException.class, prop);
      }

      checkerBuilder.endControlFlow();

      builder.addCode(checkerBuilder.build());
    }

    return builder
        .addCode(CodeBlock.builder()
            .addStatement("return new $T(" + statementList + ")", objectClass)
            .build())
        .build();
  }

  private CodeBlock copierCodeBlock(ClassName builderClass, List<ImmuProperty> properties) {
    final CodeBlock.Builder copierBlockBuilder = CodeBlock.builder();

    copierBlockBuilder.addStatement("final $T builder = new $T()", builderClass, builderClass);

    properties
        .stream()
        .map((p) -> p.name().toString())
        .forEach((n) -> copierBlockBuilder.addStatement("builder." + n + "(" + "immutable." + n + "())"));

    copierBlockBuilder.addStatement("return builder");

    return copierBlockBuilder.build();
  }

  private CodeBlock creatorCodeBlock(ClassName builderClass) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    builder.addStatement("return new $T()", builderClass);
    return builder.build();
  }
}
