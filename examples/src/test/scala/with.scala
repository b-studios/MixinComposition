package compose

object composeMacroTest {

  trait A { def a: Int }
  trait B { def b: String }
  trait C { def x: Int }
  
  type D = B with C
  Compose.compose[A, B with C]
  Compose.compose[A, D]
  Compose.compose[A with B, C]
  implicitly[Compose[A with B, C]]

  // now also works with duplicate members:
  Compose.compose[A, A]
  Compose.compose[A, B with A]
  Compose.compose[B, B with C]
}
