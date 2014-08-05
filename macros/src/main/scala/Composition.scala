package de.unimarburg
package composition

import scala.reflect.api.Universe

trait Composition extends InspectionHelpers with TemplateHelpers {

  val universe: Universe
  import universe._
 
  def materializeWith[A, B](implicit aT: WeakTypeTag[A], bT: WeakTypeTag[B]): Tree = {
    
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

    q"""new de.unimarburg.composition.With[$aT, $bT] {
      def apply($aName: $aT, $bName: $bT) = {
        $anonimpl ;
        new $constrName
      }
    }"""
  }

}
