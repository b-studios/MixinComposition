package de.unimarburg
package composition

import scala.reflect.macros.Context

trait TemplateHelpers { self: InspectionHelpers =>

  val context: Context
  import context.universe._

  def defineMember(member: Symbol, delegatee: TermName) = {
    val name = newTermName(member.name.toString)
    if (isGetter(member))
      q"""val $name = $delegatee.$name;"""
    else
      q"""def $name = $delegatee.$name;"""
  }

  def classDef(constrName: TypeName, supertypes: List[Type], members: List[ValOrDefDef]) =
    ClassDef(NoMods, constrName, Nil, Template(
      supertypes.map(t => tq"$t"),
      emptyValDef,
      defaultConstr +: members))

  def defaultConstr = DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(List()),
    TypeTree(),
    Block(List(
      Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())),
    Literal(Constant(()))))
}