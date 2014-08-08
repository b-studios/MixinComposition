package de.unimarburg
package composition

import scala.language.experimental.{ macros => scalaMacros }
import scala.reflect.macros.Context
import scala.reflect.api.Universe

package object macros {

  def mkCompiler(c: Context) = {
    new Compiler {
      val universe: c.universe.type = c.universe
      import universe._

      def fresh(name: String) = c.freshName(name)
      def typecheck(t: Tree): Tree = c.typeCheck(t)
      def untypecheck(t: Tree): Tree = c.untypecheck(t)
      def parse(s: String): Tree = c.parse(s)
    }
  }

  def Composition(c: Context) = new Composition {
    val compiler = mkCompiler(c)
  }

  def FunctorInstances(c: Context) = new FunctorInstances {
    val compiler = mkCompiler(c)
  }

  def materialize[A, B](c: Context)(implicit aT: c.WeakTypeTag[A], bT: c.WeakTypeTag[B]): c.Expr[A With B] =
    c.Expr[A With B](Composition(c).materializeWith[A, B])

  def materializeF[F[+_], G[+_]](c: Context)(implicit fT: c.WeakTypeTag[F[_]], gT: c.WeakTypeTag[G[_]]): c.Expr[F WithF G] =
    c.Expr[F WithF G](Composition(c).materializeWithF[F, G])

  def materializeFunctor[F[+_]](c: Context)(implicit fT: c.WeakTypeTag[F[_]]): c.Expr[Functor[F]] =
    c.Expr[Functor[F]](FunctorInstances(c).materializeFunctorInstance[F])

  implicit def mix[A, B]: A With B = macro macros.materialize[A, B]
  implicit def mixF[F[+_], G[+_]]: F WithF G = macro macros.materializeF[F, G]
  implicit def functor[F[+_]]: Functor[F] = macro macros.materializeFunctor[F]

  implicit object With {

    def apply[A, B]: A With B = macro macros.materialize[A, B]

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
  }

  implicit def anyBoth: Any With Any = new (Any With Any) {
    def apply(a: Any, b: Any): Any with Any = a
  }

  implicit def anyL[T]: Any With T = new (Any With T) {
    def apply(a: Any, b: T): Any with T = b
  }

  implicit def anyR[T]: T With Any = new (T With Any) {
    def apply(a: T, b: Any): T with Any = a
  }

  implicit def same[T]: T With T = new (T With T) {
    def apply(a: T, b: T): T with T = b
  }
}