package de.unimarburg
package composition

import scala.language.experimental.{ macros => scalaMacros }
import scala.reflect.macros.Context
import scala.reflect.api.Universe

package object macros {

  def materialize[A, B](c: Context)(implicit aT: c.WeakTypeTag[A], bT: c.WeakTypeTag[B]): c.Expr[A With B] = {

    val helpers = new InspectionHelpers with TemplateHelpers {
      val universe: c.universe.type = c.universe
    }

    import helpers._
    import c.universe._

    val a = newTermName(c.fresh("a"))
    val b = newTermName(c.fresh("b"))

    val abstractMembersA = abstractMembers(aT)
    val abstractMembersB = abstractMembers(bT)
    val memberDefs = abstractMembersB.map { defineMember(_, b) } ++ (abstractMembersA
      .filterNot(abstractMembersB contains _)
      .map { defineMember(_, a) })

    val types = filterOutObjectLikeThings((getTypeComponents(aT.tpe) ++ getTypeComponents(bT.tpe)).distinct)

    val constrName = newTypeName(c.fresh("Impl"))

    val anonimpl = classDef(constrName, types, memberDefs)

    c.Expr[A With B](q"""new With[$aT, $bT] {
      def apply($a: $aT, $b: $bT) = {
        $anonimpl ;
        new $constrName
      }
    }""")
  }

  implicit def mix[A, B]: A With B = macro macros.materialize[A, B]

  implicit object With {

    def apply[A, B]: A With B = macro macros.materialize[A, B]

    def apply[A, B](a: A, b: B)(implicit comp: A With B) = comp(a, b)

    // naive implementation of multi argument composition:
    def apply[A, B, C](a: A, b: B, c: C)(implicit
      comp1: A With B, comp2: (A with B) With C) = comp2(comp1(a, b), c)

    def apply[A, B, C, D](a: A, b: B, c: C, d: D)(implicit
        comp1: A With B,
        comp2: (A with B) With C,
        comp3: (A with B with C) With D) = comp3(comp2(comp1(a, b), c), d)

    def apply[A, B, C, D, E](a: A, b: B, c: C, d: D, e: E)(implicit
        comp1: A With B,
        comp2: (A with B) With C,
        comp3: (A with B with C) With D,
        comp4: (A with B with C with D) With E) = comp4(comp3(comp2(comp1(a, b), c), d), e)
  }

  implicit def anyBoth: Any With Any = new (Any With Any) {
    def apply(a: Any, b: Any): Any with Any = a
  }

  implicit def anyL[T]: Any With T = new (Any With T) {
    def apply(a: Any, b: T): Any with T = b
  }

  implicit def anyR[T]: T With Any = new (T With Any) {
    def apply(a: T, b: Any): T with Any = a
  }

  implicit def same[T]: T With T = new (T With T) {
    def apply(a: T, b: T): T with T = b
  }
}