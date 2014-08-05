package de.unimarburg
package composition

import scala.reflect.api.Universe

trait InspectionHelpers {

  val universe: Universe
  import universe._
  import scala.reflect.internal

  def isAbstract(sym: Symbol): Boolean = sym
    .asInstanceOf[internal.Symbols#Symbol]
    .hasFlag(internal.Flags.DEFERRED)

  def isGetter(sym: Symbol): Boolean = sym
    .asInstanceOf[internal.Symbols#Symbol]
    .isGetter

  def dealias(t: Type) = t
    .asInstanceOf[internal.Types#Type]
    .dealias
    .asInstanceOf[Type]

  def parents[T](tt: WeakTypeTag[T]) = tt
    .tpe
    .asInstanceOf[internal.Types#Type]
    .parents
    .asInstanceOf[List[Type]]

  /**
   * Should return components of intersection types as list
   * If type is not intersection type returns the singleton list
   */
  def getTypeComponents(t: Type): List[Type] = dealias(t) match {
    case RefinedType(parents, _) => parents.flatMap( p => getTypeComponents(p) )
    case t => List(t)
  }

  def filterOutObjectLikeThings[T](types: List[Type]): List[Type] = types.filter { t =>
    ! (t =:= typeOf[AnyRef] || t =:= typeOf[AnyVal] || t =:= typeOf[Any])
  }

  def abstractMembers[T](tt: WeakTypeTag[T]) =
    tt.tpe.members.toList.filter(isAbstract)

}
