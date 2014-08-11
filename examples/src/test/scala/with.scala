package de.unimarburg
package composition

import org.scalatest._

trait A { def a: Int }
trait B { def b: String }
trait C { def c: Int }
class D { def d: Boolean = true }
abstract class E { def e: String }

trait WithParams {
  def foo(a: Int)(b: String): String
}

trait Shared {
  type Alias = B with C

  val a = new A { def a = 42 }
  val b = new B { def b = "Hello Composition" }
  val bc = new B with C { def b = "foo"; def c = 42 }
  val params = new WithParams { def foo(a: Int)(b: String) = b + "_foo_" + a.toString }
}

class MacrosTest extends FlatSpec with Shared {

  import macros._

  mix[A, B with C]
  mix[A, Alias]
  mix[A with B, C]
  implicitly[With[A with B, C]]

  // now also works with duplicate members:
  mix[A, A]
  mix[A, B with A]
  mix[B, B with C]

  // first one can be a class
  mix[D, D]

  // with parameters
  mix[WithParams, A]
}

class ReflectionTest extends FlatSpec with Shared {

  import reflection._

  "Mixing two different traits" should "allow accessing both members" in {
    assert(mix[A, B](a, b).a == 42)
    assert(mix(a, b).b == "Hello Composition")
  }

  "Mixing a class and a trait" should "allow accessing both members" in {
    assert(mix(new D(), b).d == true)
    assert(mix(new D(), b).b == "Hello Composition")

    assert(mix(new E { def e = "foo" }, a).e == "foo")
    assert(mix(new E { def e = "foo" }, a).a == 42)
  }

  "Mixing a type alias and a trait" should "resolve the type alias" in {
    assert(mix[Alias, A](bc, a).c == 42)
  }

  "Mixing traits with methods that contain params" should "pass arguments" in {
    assert(mix(params, a).foo(42)("hello") == "hello_foo_42")
    assert(mix(params, a).a == 42)
  }
}