package de.unimarburg
package composition

import scala.tools.reflect.ToolBox

package object reflection {

  val mirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)
  val tb = mirror.mkToolBox()

  def mkCompiler = {

    new Compiler {
      val universe: mirror.universe.type = mirror.universe
      import universe._

      // freshName cannot be accessed in reflective environment...
      // reminder: compiler crashes if identifiers end in $
      // @see https://issues.scala-lang.org/browse/SI-8425
      private var id: Int = 0;
      def fresh(name: String) = { id = id + 1; "$_" + name + "_" + id }

      def typecheck(t: Tree): Tree = mirror.mkToolBox().typeCheck(t)
      def untypecheck(t: Tree): Tree = mirror.mkToolBox().untypecheck(t)
      def parse(s: String): Tree = mirror.mkToolBox().parse(s)
    }
  }

  object Composition extends Composition {
    val compiler = mkCompiler
  }
  import Composition.compiler.universe._

  def mix[A, B](a: A, b: B)(implicit aT: TypeTag[A], bT: TypeTag[B]): A with B =
    mix[A, B](aT, bT)(a, b)

  /**
   * Method that allows mixing together objects. The resulting composition
   * uses delegation to implement both interfaces.
   *
   * TODO caching
   */
  def mix[A, B](implicit aT: TypeTag[A], bT: TypeTag[B]): A With B = {
    val tb = mirror.mkToolBox()
    tb.eval(Composition.materializeWith[A, B]).asInstanceOf[A With B]
  }

  def mixF[F[+_], G[+_]](implicit fT: TypeTag[F[_]], gT: TypeTag[G[_]]): F WithF G = {
    val tb = mirror.mkToolBox()
    tb.eval(Composition.materializeWithF[F, G]).asInstanceOf[F WithF G]
  }
}