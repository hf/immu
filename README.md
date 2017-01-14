# Immu

[![Build Status](https://travis-ci.org/hf/immu.svg?branch=master)](https://travis-ci.org/hf/immu) [![codecov](https://codecov.io/gh/hf/immu/branch/master/graph/badge.svg)](https://codecov.io/gh/hf/immu)
[ ![immu-compiler](https://api.bintray.com/packages/stojan/java/immu-compiler/images/download.svg) ](https://bintray.com/stojan/java/immu-compiler/_latestVersion) [ ![immu-annotations](https://api.bintray.com/packages/stojan/java/immu-annotations/images/download.svg) ](https://bintray.com/stojan/java/immu-annotations/_latestVersion)

Immu is a small annotation processor that will allow you to never write
another builder and immutable object by hand.

Life is too short to properly code and test data-holding, immutable objects,
and the boilerplate they come with: builders, hash codes, equality, meaningful
toStrings, nullability, ...

So, one night I sat down and coded Immu. Its point is to be minimal and do
one thing well.

## How?

OK, so Immu has three annotations `@Immu`, `@SuperImmu` and `@Required`. You
can use these to describe a very strict immutable object structure.

`@Immu` and `@SuperImmu` can only be used on interfaces that only have methods
of the form `ReturnType name()`. Methods with parameters, type variables,
or exceptions are not allowed and the processor will let you know.

`@Immu` gives you:

 1. A package-protected, final implementation of the `@Immu` interface.
 2. A public, final, well-formed builder for the `@Immu` interface.

Interfaces annotated with `@Immu` or `@SuperImmu` may extend other interfaces
but they either have to be themselves annotated, or they will have to be
empty. The processor will let you know about this, too.

`@SuperImmu` is like a super-class for `@Immu`. You can use `@SuperImmu`
interfaces to encapsulate data that is shared between other `@Immu`-s, but
that data is not necessarily standalone.

`@Required` is for properties only. If a property is annotated with
`@Required` it will have to be provided in builders and constructors. For
declared types (things that are objects, basically) this means that they
must not be `null`. For primitive types, it only applies for builders:
you will have to provide a value in the builder creator.

That's basically it. No bullshit.

### Example

```java
@SuperImmu
public interface Animal {
    @Required String name();
    int eyes();
}

@SuperImmu
public interface LeggedAnimal extends Animal {
    int legs();
}

@Immu
public interface Tweeter extends LeggedAnimal {
    String twitter();
}

@Immu
public interface Octopus extends LeggedAnimal {
    @Required String favoriteOcean();
}

...

Octopus octocat = OctopusBuilder
    .create()
    .eyes(2)
    .legs(8)
    .favoriteOcean("Atlantic Ocean")
    .build(); // @Required will be checked here

Tweeter chirpy = TweeterBuilder
    .create()
    .twitter(/* may be null */ "@archillect")
    .eyes(2)
    .legs(2)
    .build();

Octopus octocatsBro = OctopusBuilder
    .from(octocat)
    .name("Octocat's Bro")
    .build();
```

### API freeze

No matter how much the implementation of the compiler (annotation processor) 
changes in the future, these are the APIs that will **always** be exposed by 
Immu.

Builders will always have the suffix `Builder` from the interface name.

```java
// a Builder for an @Immu annotated interface
public final class Builder {
  // creates a new builder and initializes all builder values from the provided immu
  public static Builder from(Immu immu) { /* ... */ }
  
  // creates a new empty builder
  public static Builder create() { /* ... */ }
  
  // property setter for reference types / declared types
  public Builder propertyName(PropertyType value) { /* ... */ }
  
  // property setter for primitive types
  public Builder propertyName(primitive_type value) { /* ... */ }
  
  // builds a new Immu object from the values provided here, if there are any
  // @Required properties, this method will throw a ValueNotProvidedException
  // that will explain which property was not provided
  public Object build() { /* ... */ }
}
```

Implementations of the immutable interface will always be package-protected,
and will always be prefixed with `Immutable` onto the interface name.

```java
// the immutable object implementation class
/* package-protected */ final class Immutable implements TheImmuInterface {
  
  // constructor for all values, will throw a ValueNotProvidedException if a
  // reference / declared property has been annotated with @Required and was
  // provided a value that is null
  Immutable(/* ... */) { /* ... */ }
  
  // getter for primitive type properties
  @Override public primitive_type propertyPrimitive() { /* ... */ }
  
  // getter for reference / declared properties
  @Override public PropertyType propertyReference() { /* ... */ }
  
  // a standards compliant hash code, that is an XOR of all property values
  // and most importantly, the starting value is TheImmuInterface.class.hashCode()
  @Override public int hashCode() { /* ... */ }
  
  // an equals implementation that does equality checks on the TheImmuInterface, 
  // and not on the generated class
  @Override public boolean equals(Object object) { /* ... */ }
}
```

Other features may be present, per release. However, these features will always
be available and will **never** change. 

### Building, Contributing

Building requires JDK8. It is recommended you use versions *above* 
`1.8.0_31` since there have been issues with hanging compiler tests including
troubles with inferring types in the code that uses the Java 8 stream APIs.

The tests exercise the full processor, and not every method is directly tested 
since that would be impossible without forking `javac`. Therefore, the 
processor uses the excellent `com.google.testing.compile:compile-testing` 
library from Google.

# License

Copyright &copy; 2017 Stojan Dimitrovski

Licensed under the MIT X11 License. See `LICENSE.txt` for the full text.
