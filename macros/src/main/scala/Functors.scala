package de.unimarburg
package composition

import scala.reflect.api.Universe

trait Functor[F[_]] {
  type Apply[A] = F[A]
  def map[A, B](f: A => B, fa: Apply[A]): Apply[B]
}

/**
 * Generates functor instance Functor[F] for a given type constructor F[_]
 */
trait FunctorInstances extends InspectionHelpers with TemplateHelpers {

  val compiler: Compiler

  import compiler._
  import universe._

  def materializeFunctorInstance[F[+_]](implicit fT: WeakTypeTag[F[_]]): Tree = {

    val fC = fT.tpe.typeConstructor

    val faName = newTermName(fresh("fa"))

    val List(paramS) = fC.typeParams
    val paramT = typeOfSym(paramS)

    val (aTName, bTName, fName) = (newTypeName(fresh("A")), newTypeName(fresh("B")), newTermName(fresh("f")))

    def buildFunctorInstance(impl: Tree): Tree =
      q"""new de.unimarburg.composition.Functor[$fC] {
          def map[$aTName, $bTName]($fName: $aTName => $bTName, $faName: Apply[$aTName]): Apply[$bTName] = $impl
        }"""

    // construct a dummy and type check it, same trick as in `Composition.scala`
    val dummyT = typecheck(buildFunctorInstance(q"???")).tpe

    // analyze the parts of the type checked result
    val method = dummyT.member("map": TermName).asMethod

    val List(aT, bT) = method.typeParams.map(typeOfSym)

    val (faT, fbT) = (appliedType(fC, aT), appliedType(fC, bT))

    val (reqMap, plain) = abstractMembers(fbT).partition {
      case m if m.isMethod => m.asMethod.returnType =:= paramT
      case m => typeOfSym(m).resultType =:= paramT
    }

    val memberDefs = (plain.map(defineMember(_, faName)) ++

       // wrap into call to f(...)
      reqMap.map(member =>
        defineMember(member, faName) match {
          case DefDef(mods, name, tps, vpss, tpt, body) =>
            DefDef(mods, name, tps, vpss, tpt, q"${fName}($body)")
          case ValDef(mods, name, tpt, body) =>
            ValDef(mods, name, tpt, q"${fName}($body)")
        }))

    val constrName = newTypeName(fresh("Impl"))
    val anonimpl = classDef(constrName, fbT :: Nil, memberDefs)
    buildFunctorInstance(parse(showCode(q"{ $anonimpl; new $constrName }")))
  }
}