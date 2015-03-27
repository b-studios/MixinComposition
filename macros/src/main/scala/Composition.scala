package de.unimarburg
package composition

import scala.reflect.api.Universe

trait Composition extends InspectionHelpers with TemplateHelpers { self =>

  val compiler: Compiler

  import compiler._
  import universe._

  def materializeWithF[F[+_], G[+_]](implicit fT: WeakTypeTag[F[_]], gT: WeakTypeTag[G[_]]): Tree = {

    val fC = fT.tpe.typeConstructor
    val gC = gT.tpe.typeConstructor

    val fName = newTermName(fresh("fa"))
    val gName = newTermName(fresh("ga"))

    val aTName = newTypeName(fresh("A"))

    def buildWithF(impl: Tree): Tree =
      q"""new de.unimarburg.composition.WithF[$fC, $gC] {
          def apply[$aTName]($fName: ApplyF[$aTName], $gName: ApplyG[$aTName]): Apply[$aTName] = $impl
        }"""

    // construct a dummy and type check it
    val dummy = buildWithF(q"???")
    val resT = typecheck(dummy)

    // analyze the parts of the type checked result
    val method = resT.tpe.member("apply": TermName).asMethod

    val fParam = method.paramLists.head(0)
    val gParam = method.paramLists.head(1)

    val fParamT = typeOfSym(fParam)
    val gParamT = typeOfSym(gParam)

    // THIS IS IMPORTANT!
    // ------------------
    // We cannot invoke parse(showCode(...)) on the tree as a whole (resT or dummy)
    // since it fails with dealiased types that represent type lambdas
    // like ({ type λ[X] = F[X] })#λ
    //
    // printing and parsing hacks around the following type error
    //
    //     type mismatch;
    //     [error]  found   : A(in method apply)(in method apply) ...
    //     [error]  required: A(in method apply)(in method apply) ...
    val composed = composedInstance(fName, gName, fParamT, gParamT)

    buildWithF(composed)
  }


  def materializeWith[A, B](implicit aT: WeakTypeTag[A], bT: WeakTypeTag[B]): Tree = {

    val aName = newTermName(fresh("a"))
    val bName = newTermName(fresh("b"))

    // shortcuts for Any and AnyRef
    val composed = if (isObjectLikeThing(aT.tpe)) {
      q"""$bName"""
    } else if (isObjectLikeThing(bT.tpe)) {
      q"""$aName"""
    } else {
      composedInstance(aName, bName, aT.tpe, bT.tpe)
    }

    q"""new de.unimarburg.composition.With[$aT, $bT] {
      def apply($aName: $aT, $bName: $bT) = $composed
    }"""
  }

  def composedInstance(aName: TermName, bName: TermName, aT: Type, bT: Type) = {
    val abstractMembersA = abstractMembers(aT)
    val abstractMembersB = abstractMembers(bT)
    val memberDefs = abstractMembersB.map { defineMember(_, bName) } ++ (abstractMembersA
      .filterNot(abstractMembersB contains _)
      .map { defineMember(_, aName) })

    val types = filterOutObjectLikeThings((getTypeComponents(aT) ++ getTypeComponents(bT)).distinct)

    val superClasses = types.map(_.toString) mkString " with "
    val memberString = memberDefs.map(m => m.toString) mkString ";\n"
    parse(s"{ new $superClasses { $memberString } }")
  }
}
