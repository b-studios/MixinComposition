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

    // find all type params in superclasses
    val typeParams = for {
      cls <- fT.tpe.baseClasses
      tc = typeOfSym(cls).typeConstructor
      if tc.takesTypeArgs
      param <- tc.typeParams
    } yield typeOfSym(param)


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
      case m if m.isMethod => typeParams contains m.asMethod.returnType
      case m => typeParams contains typeOfSym(m).resultType
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
    val superTypes = filterOutObjectLikeThings(getTypeComponents(fbT).distinct)

    // this seems wrong, but calling showCode(tq"$fbT") does sometimes
    // emit an empty string which is then interpreted as AnyRef
    val className = superTypes.map(_.toString) mkString " with " //fbT.toString

    // toString actually works better than showCode here.
    // showCode sometimes did not print types in method declarations...
    val memberString = memberDefs.map(m => m.toString) mkString ";\n"

    // putting everything together as a string seems fragile, but it
    // removes conflicting type information and also works better then
    // the combination of showCode and quasiquotes
    val implstr = s"{ new $className { $memberString } }"
    buildFunctorInstance(parse(implstr))
  }
}