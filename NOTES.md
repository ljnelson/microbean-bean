# Notes

## Model

For every `Factory`:
* Grab its dependencies
* Ask a `Selectable` to 

## Obsolete, or Unmaintained

### Factories, Producers, etc.

A `Factory` is the user-visible creation mechanism.

It is usually comprised of:

* A `Producer`. This creates the product, in a possibly intercepted manner, resolving its creational dependencies as
  necessary.
 * Because creating the product may be intercepted, you can break this down conceptually into:
  * Dependency resolution. Something has to get the initial parameters for whatever the means of production is.
   * These will either be directly supplied to the means of production or via a type conversion machine (e.g. `Object[]`)
     to the construction interceptor machinery
  * Instantiation. The thing that actually does the instantiation in the absence of construction 
  
  
  
  
```
Producer:

produce();

Intercepting producer:

public class InterceptingProducer {
  private final Chain c;
  public InterceptingProducer(Chain c) {
    super();
    this.c = c;
  }
  @Override
  public Object produce() {
    return this.c.call();
  }
}
```

Instead of taking a `Chain` it should take the raw materials to make a chain that sets its target:

* A `List` of `InterceptorMethod`s
* A "terminal function" (`Function<? super Object[], ?>`)
* A supplier of arguments (`Supplier<? extends Object[]>`)

This also performs constructor interception.

** We can break this down further into an "instantiator", which comes up with construction arguments via dependency
   resolution, and an interceptor that intercepts these arguments and


** This _may_ (haven't decided yet) decompose into something that does the resolution, and the "instantiator", whose job
   is simply to `new`.
* An `Initializer`. This calls the product's initializer methods.
* A `PostInitializer`. This calls the product's `postConstruct()` callbacks.
* A `PreDestructor`. This calls the product's `preDestroy()` callbacks.

### `AutoCloseableRegistry` Instances

An `AutoCloseableRegistry` is an `AutoCloseable` and a collection of `AutoCloseable`s. Therefore it is also a tree.

All such trees in the system descend from a primordial root. There is therefore only one tree, not many.

