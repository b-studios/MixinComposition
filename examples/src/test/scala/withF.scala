package de.unimarburg
package composition

import org.scalatest._

object functorTests {

  trait F[+A] {
    def foo: A
  }

  trait G[+A] {
    def bar: A
  }

  trait H[+A] {
    def baz: A
  }

}

class ReflectionTest2 extends FlatSpec with Shared {

   import reflection._
   import functorTests._

   val bothf = mixF[F, G]

   val both = bothf[Int](new F[Int] { def foo = 1 }, new G[Int] { def bar = 2})
   assert(both.foo == 1)
   assert(both.bar == 2)

   val threef = mixF[(F WithF G)#Apply, H]
   val three = threef(both, new H[Int] { def baz = 3 })
   assert(three.foo == 1)
   assert(three.bar == 2)
   assert(three.baz == 3)
}