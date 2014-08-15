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
}