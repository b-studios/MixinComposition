package de.unimarburg
package composition

object composeMacroTest {

  trait A { def a: Int }
  trait B { def b: String }
  trait C { def x: Int }

  type D = B with C
  With.compose[A, B with C]
  With.compose[A, D]
  With.compose[A with B, C]
  implicitly[With[A with B, C]]

  // now also works with duplicate members:
  With.compose[A, A]
  With.compose[A, B with A]
  With.compose[B, B with C]
}
