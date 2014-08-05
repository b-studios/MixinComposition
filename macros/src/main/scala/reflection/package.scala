package de.unimarburg
package composition

import scala.tools.reflect.ToolBox

package object reflection {

  private val mirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)
  private val helpers = new InspectionHelpers with TemplateHelpers {
    val universe = mirror.universe
  }

  import helpers.universe._

  /**
   * Method that allows mixing together objects. The resulting composition
   * uses delegation to implement both interfaces.
   *
   * TODO caching
   */
  def mix[A, B](a: A, b: B)(implicit aT: TypeTag[A], bT: TypeTag[B]): A with B = {

    import helpers._
    import universe._

    val tb = mirror.mkToolBox()

    // freshName cannot be accessed in reflective environment...
    // reminder: compiler crashes if identifiers end in $
    // @see https://issues.scala-lang.org/browse/SI-8425
    def fresh(name: String) = "$_" + name + "_"

    val aName = newTermName(fresh("a"))
    val bName = newTermName(fresh("b"))

    val abstractMembersA = abstractMembers(aT)
    val abstractMembersB = abstractMembers(bT)
    val memberDefs = abstractMembersB.map { defineMember(_, bName) } ++ (abstractMembersA
      .filterNot(abstractMembersB contains _)
      .map { defineMember(_, aName) })

    val types = filterOutObjectLikeThings((getTypeComponents(aT.tpe) ++ getTypeComponents(bT.tpe)).distinct)

    val constrName = newTypeName(fresh("Impl"))

    val anonimpl = classDef(constrName, types, memberDefs)

    val tree = q"""new de.unimarburg.composition.With[$aT, $bT] {
      def apply($aName: $aT, $bName: $bT) = {
        $anonimpl ;
        new $constrName
      }
    }"""
    tb.eval(tree).asInstanceOf[A With B](a, b)
  }
}