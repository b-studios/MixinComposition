package de.unimarburg
package composition

trait TemplateHelpers { self: InspectionHelpers =>

  val compiler: Compiler
  import compiler.universe._

  def defineMember(member: Symbol, delegatee: TermName): ValOrDefDef =
    defineMember(member, q"$delegatee")

  def defineMember(member: Symbol, delegatee: Tree): ValOrDefDef = {
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