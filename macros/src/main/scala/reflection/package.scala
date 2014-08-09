package de.unimarburg
package composition

import scala.tools.reflect.ToolBox

trait ReflectionImpl extends Composition with FunctorInstances {

  def mirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)
  def toolbox = mirror.mkToolBox()

  lazy val compiler = new Compiler {
    val mr = mirror
    val tb = mr.mkToolBox()

    val universe: mr.universe.type = mr.universe
    import universe._

    // freshName cannot be accessed in reflective environment...
    // reminder: compiler crashes if identifiers end in $
    // @see https://issues.scala-lang.org/browse/SI-8425
    private var id: Int = 0;
    def fresh(name: String) = { id = id + 1; "$_" + name + "_" + id }

    def typecheck(t: Tree): Tree = tb.typeCheck(t)
    def untypecheck(t: Tree): Tree = tb.untypecheck(t)
    def parse(s: String): Tree = tb.parse(s)
  }
}

package object reflection extends ReflectionImpl {

  import compiler.universe._

  implicit def functor[F[+_]](implicit fT: WeakTypeTag[F[_]]): Functor[F] =
    toolbox.eval(materializeFunctorInstance[F]).asInstanceOf[Functor[F]]

  def mix[A, B](a: A, b: B)(implicit aT: WeakTypeTag[A], bT: WeakTypeTag[B]): A with B =
    mix[A, B](aT, bT)(a, b)

  /**
   * Method that allows mixing together objects. The resulting composition
   * uses delegation to implement both interfaces.
   *
   * TODO caching
   */
  implicit def mix[A, B](implicit aT: WeakTypeTag[A], bT: WeakTypeTag[B]): A With B =
    toolbox.eval(materializeWith[A, B]).asInstanceOf[A With B]

  implicit def mixF[F[+_], G[+_]](implicit fT: WeakTypeTag[F[_]], gT: WeakTypeTag[G[_]]): F WithF G =
    toolbox.eval(materializeWithF[F, G]).asInstanceOf[F WithF G]
}