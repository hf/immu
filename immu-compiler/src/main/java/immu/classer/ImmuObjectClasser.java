package immu.classer;

import com.squareup.javapoet.*;
import immu.Required;
import immu.Immutable;
import immu.ValueNotProvidedException;
import immu.element.ImmuObjectElement;
import immu.element.ImmuProperty;

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
        .addJavadoc(CodeBlock.builder()
            .add("Construct a new immutable object. Only copies and checks for null values of the provided arguments.\n")
            .add("@see $T#build()\n", builderClass())
            .add("@throws $T if a property is annotated as {@link $T} but has been given a value of null\n", ValueNotProvidedException.class, Required.class)
            .build())
        .build();

    final MethodSpec hashCode = MethodSpec.methodBuilder("hashCode")
        .addModifiers(Modifier.PUBLIC)
        .returns(int.class)
        .addAnnotation(Override.class)
        .addCode(hashCodeBlock(immuClass, properties))
        .addJavadoc(CodeBlock.builder()
            .add("Computes the hash code for this object. This is an XOR operation of the hash codes of all properties")
            .add("in {@link $T} as well as {@code $T.class.getCanonicalName().hashCode()}.\n", immuClass, immuClass)
            .add("@return the hash code\n")
            .build())
        .build();

    final MethodSpec toString = MethodSpec.methodBuilder("toString")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addAnnotation(Override.class)
        .addCode(toStringBlock(immuClass, properties))
        .addJavadoc(CodeBlock.builder()
            .add("Constructs a string representing the immutable object described in {@link $T}.\n", immuClass)
            .add("<p>\nThe format of the string will be:\n")
            .add("<pre>$T@0abcdefa{ propertyName = &lt;VALUE&gt;, propertyName = @null }</pre>\n", immuClass)
            .add("<p>\nThe order of the properties will be the same as defined in {@link $T}.\n", immuClass)
            .add("<p>\n{@code @null} means the value was null, and {@code <null>} means that there was an object who's toString evaluated to {@code \"null\"}.\n")
            .add("@return the string representation, never null\n")
            .build())
        .build();

    final MethodSpec equals = MethodSpec.methodBuilder("equals")
        .addModifiers(Modifier.PUBLIC)
        .returns(boolean.class)
        .addAnnotation(Override.class)
        .addParameter(Object.class, "object")
        .addCode(equalsBlock(immuClass, properties))
        .addJavadoc(CodeBlock.builder()
            .add("Checks whether the provided object is equal to this object.\n")
            .add("<p>\nDiffers slightly from the normal Java convention in that it will consider the provided object")
            .add("as equal if and only if it is an instance of {@link $T}.\n", immuClass)
            .add("<p>\nAfterwards, all properties are being compared for equality.\n")
            .add("@return if the objects are equal\n")
            .build())
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

    final MethodSpec clear = MethodSpec.methodBuilder("clear")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addCode(CodeBlock.builder()
            .addStatement("this.computedToString = null")
            .build())
        .returns(void.class)
        .addJavadoc(CodeBlock.builder()
            .add("Clears the cached computed {@link #toString()} value.\n")
            .build())
        .build();

    return TypeSpec.classBuilder(objectClass)
        .addModifiers(Modifier.FINAL)
        .addTypeVariables(typeVariables)
        .addSuperinterface(immuClass)
        .addSuperinterface(Immutable.class)
        .addFields(fields)
        .addField(FieldSpec.builder(int.class, "computedHashCode", Modifier.PRIVATE, Modifier.TRANSIENT, Modifier.VOLATILE).build())
        .addField(FieldSpec.builder(String.class, "computedToString", Modifier.PRIVATE, Modifier.TRANSIENT, Modifier.VOLATILE).build())
        .addMethod(constructor)
        .addMethods(methods)
        .addMethod(hashCode)
        .addMethod(equals)
        .addMethod(toString)
        .addMethod(clear)
        .addJavadoc(CodeBlock.builder()
            .add("An immutable implementation of {@link $T}.\n", immuClass)
            .add("<p>\nYou should avoid usage of this class, and instead prefer using the {@link $T}.\n", builderClass())
            .add("@see $T\n", immuClass)
            .build())
        .build();
  }

  private CodeBlock toStringBlock(ClassName immuClass, List<ImmuProperty> properties) {
    final CodeBlock.Builder builder = CodeBlock.builder()
          .addStatement("final $T existingToString = this.computedToString", String.class)
          .beginControlFlow("if (null != existingToString)")
          .addStatement("return existingToString")
          .endControlFlow()
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

    builder.addStatement("final $T generatedToString = builder.toString()", String.class);
    builder.addStatement("this.computedToString = generatedToString");

    return builder.addStatement("return generatedToString")
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

    builder.addStatement("final int existingHashCode = this.computedHashCode");
    builder.beginControlFlow("if (0 != existingHashCode)");
    builder.addStatement("return existingHashCode");
    builder.endControlFlow();

    if (properties.isEmpty()) {
      builder.addStatement("final int hashCode = $T.class.getCanonicalName().hashCode()", immuClass);
      builder.addStatement("this.computedHashCode = hashCode");
      builder.addStatement("return hashCode");

      return builder.build();
    }

    builder.addStatement("int hashCode = $T.class.getCanonicalName().hashCode()", immuClass);

    for (ImmuProperty property : properties) {
      hashCodeInvocation(property, builder);
    }

    builder.addStatement("this.computedHashCode = hashCode");

    builder.addStatement("return hashCode");

    return builder.build();
  }

  private void hashCodeInvocation(ImmuProperty property, CodeBlock.Builder builder) {
    final String value = "this." + property.name();
    final TypeKind kind = property.returnType().getKind();

    switch (kind) {
      case ARRAY:
        builder.addStatement("hashCode ^= $T.hashCode(" + value + ")", Arrays.class);
        return;

      case INT:
      case CHAR:
      case BYTE:
      case SHORT:
        builder.addStatement("hashCode ^= " + value);
        return;

      case BOOLEAN:
        builder.addStatement("hashCode ^= (" + value + "? 1 : 0)");
        return;

      case LONG:
        builder.addStatement("hashCode ^= (int) (" + value + " >> 32)");
        builder.addStatement("hashCode ^= (int) " + value);
        return;

      case FLOAT:
        builder.addStatement("hashCode ^= $T.floatToIntBits(" + value + ")", Float.class);
        return;

      case DOUBLE:
        builder.addStatement("hashCode ^= (int) ($T.doubleToLongBits(" + value + ") >> 32)", Double.class);
        builder.addStatement("hashCode ^= (int) $T.doubleToLongBits(" + value + ")", Double.class);
        return;

      default:
        if (property.isRequired()) {
          builder.addStatement("hashCode ^= " + value + ".hashCode()");
          return;
        }

        builder.addStatement("hashCode ^= $T.hashCode(" + value + ")", Objects.class);
        return;
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
