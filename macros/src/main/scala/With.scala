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

trait WithF[F[+_], G[+_]] {
  type Apply[+A] = F[A] with G[A]
  type ApplyF[A] = F[A]
  type ApplyG[A] = G[A]
  def apply[A](fa: ApplyF[A], ga: ApplyG[A]): Apply[A]
}