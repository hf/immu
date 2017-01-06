# Immu

Immu is a small annotation processor that will allow you to never write
another builder and immutable object by hand.

Life is too short to properly code and test data-holding, immutable objects,
and the boilerplate they come with: builders, hash codes, equality, meaningful
toStrings, nullability, ...

So, one night I sat down and coded Immu. Its point is to be minimal and do
one thing well.

## How?

OK, so Immu has three annotations `@Immu`, `@SuperImmu` and `@NonNull`. You
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

That's basically it. No bullshit.

### Example

```java
@SuperImmu
public interface Animal {
    @NonNull String name();
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
    @NonNull String favoriteOcean();
}

...

Octopus octocat = OctopusBuilder
    .create(/* name: */ "Octocat", /* favoriteOcean: */ "Pacific Ocean")
    .eyes(2)
    .legs(8)
    .build();

Tweeter chirpy = TweeterBuilder
    .create(/* name: */ "Chirpy")
    .twitter(/* may be null */ "@archillect")
    .eyes(2)
    .legs(2)
    .build();

Octopus octocatsBro = OctopusBuilder
    .from(octocat)
    .name("Octocat's Bro")
    .build();
```

# License

Copyright &copy; 2017 Stojan Dimitrovski

Licensed under the MIT X11 License. See `LICENSE.txt` for the full text.
