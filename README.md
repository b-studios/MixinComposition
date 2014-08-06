MixinComposition
===============
This small library allows composition of objects at runtime by the means of
delegation.

Originally it only consisted of the macro implementation and was developed as part of our paper:

    Rendel, Brachthäuser, Ostermann.
    From Object Algebras to Attribute Grammars.
    Proc. of Object-Oriented Programming, Systems, Languages & Applications 2014


Usage
-----
Given the two simple traits `Foo` and `Bar`

~~~scala
trait Foo { def foo: Int }
trait Bar { def bar: String }
~~~

and the instances `f` and `b`

~~~scala
val f = new Foo { def foo = 42 }
val b = new Bar { def bar = "Hello Composition" }
~~~

we can compose the two instances by using the composition library:

~~~scala
import de.unimarburg.composition.macros._

val both = mix(f, b)
println(both.foo) // yields 42
println(both.bar) // yields "Hello Composition"
~~~

Under the hood the call to `mix` will create an instance `With[Foo, Bar]`. The
definition of With is as follows:

~~~scala
trait With[A, B] {
  type Apply = A with B
  def apply(a: A, b: B): Apply
}
~~~

An instance of this typeclass witnesses that `A` and `B` can be composed. The most common use of the type `With` is infix in order to mimic Scala's syntax for intersection types `with`: `A With B`.

The automatically summoned instance of `With[Foo, Bar]` looks like:

~~~scala
val canComposeFooWithBar = new (Foo With Bar) {
  def apply(a: Foo, b: Bar) = new Foo with Bar {
    def foo = a.foo
    def bar = b.bar
  }
}
~~~

All abstract methods and fields are implemented by delegation.

### Important note

> The composition `A With B` is right biased. As such, methods in `B` will
> override equal methods in `A`.


Macros or Reflection?
---------------------
We provide two different styles to use this composition library. One is based
on macros while the other one is based on reflection. The main difference is
that with macros the instances of `With` are materialized at compile time, while
with reflection they are built at runtime.

### Macros
In order to use the macro implementation the package `macros` has to be imported

~~~scala
import de.unimarburg.composition._
import macros._
~~~

If the types `A` and `B` to be composed are not known at compile time the following pattern can be used to require materialization of With at the call site:

~~~scala
def canCompose[A, B](a: A, b: B, ...)(implicit ev: A With B) = {
  ...
  mix(a, b)
  ...
}
~~~

This way, when `canCompose` is called with particular `A` and `B` the implicit materialization kicks in and provides an instance of `A With B`.


### Reflection
In order to use the reflection based implementation the package `reflection` has to be imported

~~~scala
import de.unimarburg.composition._
import reflection._
~~~

At first the reflection based implementation seems to be more flexible since the types to be composed must not be known at compile time. However, in order to generate instances of `A With B` [TypeTags](http://docs.scala-lang.org/overviews/reflection/typetags-manifests.html) for `A` and `B` are required.

If `A` and `B` are not known at compile time their TypeTags have to be passed in a style similar to the one for macros:

~~~scala
def canCompose[A: TypeTag, B: TypeTag](a: A, b: B, ...) = {
  ...
  mix(a, b)
  ...
}
~~~

Hence, the main advantage of reflection over macros is the individual handling
of `TypeTag[A]` and `TypeTag[B]`. This becomes visible when currying `canCompose`

~~~scala
def canCompose[A: TypeTag](a: A) = new {
  def apply[B: TypeTag](b: B, ...) = {
    ...
    mix(a, b)
    ...
  }
}
~~~

Due to limitations in the implementation, currently `A` and `B` both have to
be stable types. Thus, it is not possible of using the reflection based
implementation within a [cake](http://jonasboner.com/2008/10/06/real-world-scala-dependency-injection-di/).