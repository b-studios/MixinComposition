package de.unimarburg
package composition

trait TemplateHelpers { self: InspectionHelpers =>

  val compiler: Compiler
  import compiler.universe._

  def defineMember(member: Symbol, delegatee: TermName): ValOrDefDef =
    defineMember(member, q"$delegatee")

  // currently type params, modifiers and defaults are not supported
  def defineMember(member: Symbol, delegatee: Tree): ValOrDefDef = {
    val name = newTermName(member.name.toString)
    if (isGetter(member))
      q"""val $name = $delegatee.$name;"""
    else {
      val paramss = member.asMethod.paramss

      val paramsDefs = paramss.map(_.map(defineParam))

      val body = paramss.foldLeft[Tree](q"$delegatee.$name") {
        case (body, paramList) => q"$body(..${paramList.map(_.name)})"
      }

      DefDef(Modifiers(), name, Nil, paramsDefs, TypeTree(), body)
    }

  }

  def defineParam(param: Symbol): ValDef = {
    val name = newTermName(param.name.toString)
    val tpt = tq"${typeOfSym(param)}"
    ValDef(Modifiers(Flag.PARAM), name, tpt, EmptyTree)
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