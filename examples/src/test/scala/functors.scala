package de.unimarburg
package composition

import org.scalatest._

object functorInstanceFixtures {

  trait F[+A] {
    def foo: A
    def other: Int
  }

  object IntF extends F[Int] {
    def foo = 42
    def other = 43
  }

}

class FunctorInstanceReflectionTest extends FlatSpec with Shared {

   import reflection._
   import functorInstanceFixtures._

   val f = functor[F]
   //val f = implicitly[Functor[F]]

   val StringF = f.map[Int, String](_.toString, IntF)
   assert(StringF.foo == "42")
   assert(StringF.other == 43)
}

class FunctorInstanceMacrosTest extends FlatSpec with Shared {

   import macros._
   import functorInstanceFixtures._

   val f = functor[F]
   //val f = implicitly[Functor[F]]

   val StringF = f.map[Int, String](_.toString, IntF)
   assert(StringF.foo == "42")
   assert(StringF.other == 43)
}