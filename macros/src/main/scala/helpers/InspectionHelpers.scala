package de.unimarburg
package composition

trait InspectionHelpers {

  val compiler: Compiler

  import compiler.universe._
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

  def isFinal(sym: Symbol): Boolean = sym
    .asInstanceOf[internal.Symbols#Symbol]
    .hasFlag(internal.Flags.FINAL)

  def typeOfSym(s: Symbol): Type =
    s.asInstanceOf[scala.reflect.internal.Symbols#Symbol].tpe.asInstanceOf[Type]

  /**
   * Should return components of intersection types as list
   * If type is not intersection type returns the singleton list
   */
  def getTypeComponents(t: Type): List[Type] = dealias(t) match {
    case RefinedType(parents, _) => parents.flatMap( p => getTypeComponents(p) )
    case t => List(t)
  }

  def isObjectLikeThing(t: Type): Boolean =
    t =:= typeOf[AnyRef] || t =:= typeOf[AnyVal] || t =:= typeOf[Any]

  def filterOutObjectLikeThings(types: List[Type]): List[Type] =
    types filterNot isObjectLikeThing

  def abstractMembers[T](tt: WeakTypeTag[T]): List[Symbol] =
    abstractMembers(tt.tpe)

  def abstractMembers(t: Type): List[Symbol] =
    t.members.toList.filter(isAbstract)

}
