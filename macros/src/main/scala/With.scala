package de.unimarburg
package composition

/**
 * `With` is a type class that can be used for the evidence that two types `A` and `B`
 * can be composed. It has a method `apply` that, provided instances of `A` and `B`
 * creates the intersection `A with B` by delegation.
 *
 * Contract: This composition is right biased. As such, equally named methods in `B`
 * will always override methods in `A`.
 *
 * For automatically generating instance of With there exist two implementations:
 *
 * 1. Macro based
 * 2. Reflection based
 */
trait With[A, B] {
  type Apply = A with B
  def apply(a: A, b: B): Apply
}
object With {

  def apply[A, B](implicit comp: A With B): A With B = comp

  def apply[A, B](a: A, b: B)(implicit comp: A With B) = comp(a, b)

  // naive implementation of multi argument composition:
  def apply[A, B, C](a: A, b: B, c: C)(implicit
    comp1: A With B, comp2: (A with B) With C) = comp2(comp1(a, b), c)

  def apply[A, B, C, D](a: A, b: B, c: C, d: D)(implicit
      comp1: A With B,
      comp2: (A with B) With C,
      comp3: (A with B with C) With D) = comp3(comp2(comp1(a, b), c), d)

  def apply[A, B, C, D, E](a: A, b: B, c: C, d: D, e: E)(implicit
      comp1: A With B,
      comp2: (A with B) With C,
      comp3: (A with B with C) With D,
      comp4: (A with B with C with D) With E) = comp4(comp3(comp2(comp1(a, b), c), d), e)

  implicit def same[S, T <: S]: S With T = new (S With T) {
    def apply(a: S, b: T): T = b
  }

  implicit def anyBoth: Any With Any = new (Any With Any) {
    def apply(a: Any, b: Any): Any = a
  }

  implicit def anyL[T]: Any With T = new (Any With T) {
    def apply(a: Any, b: T): T = b
  }

  implicit def anyR[T]: T With Any = new (T With Any) {
    def apply(a: T, b: Any): T = a
  }
}

trait WithF[F[+_], G[+_]] {
  type Apply[+A] = F[A] with G[A]
  type ApplyF[A] = F[A]
  type ApplyG[A] = G[A]
  def apply[A](fa: ApplyF[A], ga: ApplyG[A]): Apply[A]
}
object WithF {

  type AnyF[+X] = AnyRef

  implicit def same[F[+_], G[+X] <: F[X]]: F WithF G = new (F WithF G) {
    def apply[A](a: F[A], b: G[A]): G[A] = b
  }

  implicit def anyBoth: AnyF WithF AnyF = new (AnyF WithF AnyF) {
    def apply[A](a: AnyF[A], b: AnyF[A]): AnyF[A] = b
  }

  implicit def anyL[F[+X] <: AnyF[X]]: AnyF WithF F = new (AnyF WithF F) {
    def apply[A](a: AnyF[A], b: F[A]): F[A] = b
  }

  implicit def anyR[F[+X] <: AnyF[X]]: F WithF AnyF = new (F WithF AnyF) {
    def apply[A](a: F[A], b: AnyF[A]): F[A] = a
  }
}