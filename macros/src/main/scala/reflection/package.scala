package de.unimarburg
package composition

import scala.tools.reflect.ToolBox

package object reflection {

  private val mirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)
  object Composition extends Composition {
    val universe = mirror.universe
  }

  import Composition.universe._

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
}