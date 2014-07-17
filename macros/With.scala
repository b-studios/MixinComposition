package composition

import scala.language.experimental.macros
import scala.reflect.macros.Context

/**
 * `With` is a type class that can be used for the evidence that two types `A` and `B`
 * can be composed. It has a method `apply` that, provided instances of `A` and `B`
 * creates the intersection `A with B` by delegation.
 *
 * This composition is right biased. As such, equally named methods in `B` will always 
 * override methods in `A`.
 */
trait With[A, B] {
  type Apply = A with B
  def apply(a: A, b: B): Apply
}
object With {

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

  // maybe introduce some caching mechanism
  implicit def compose[A, B]: A With B = macro With.composeImpl[A, B]

  def composeImpl[A, B](c: Context)(implicit aT: c.WeakTypeTag[A], bT: c.WeakTypeTag[B]): c.Expr[A With B] = {
    import c.universe._
    
    def isAbstract(sym: Symbol): Boolean = sym
      .asInstanceOf[scala.reflect.internal.Symbols#Symbol]
      .hasFlag(scala.reflect.internal.Flags.DEFERRED)

    def isGetter(sym: Symbol): Boolean = sym
      .asInstanceOf[scala.reflect.internal.Symbols#Symbol]
      .isGetter

    def dealias(t: Type) = t
      .asInstanceOf[scala.reflect.internal.Types#Type]
      .dealias
      .asInstanceOf[Type]

    def parents[T](tt: WeakTypeTag[T]) = tt
      .tpe
      .asInstanceOf[scala.reflect.internal.Types#Type]
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

    val a = newTermName(c.fresh("a"))
    val b = newTermName(c.fresh("b"))

    def abstractMembers[T](tt: WeakTypeTag[T]) = 
       tt.tpe.members.toList.filter(isAbstract)

    def defineMember(member: Symbol, delegatee: TermName) = {
      val name = newTermName(member.name.toString)
      if (isGetter(member))
        q"""val $name = $delegatee.$name;"""
      else
        q"""def $name = $delegatee.$name;"""
    }

    val abstractMembersA = abstractMembers(aT)
    val abstractMembersB = abstractMembers(bT)
    val memberDefs = abstractMembersB.map { defineMember(_, b) } ++ (abstractMembersA
      .filterNot(abstractMembersB contains _)
      .map { defineMember(_, a) })

    val types = filterOutObjectLikeThings((getTypeComponents(aT.tpe) ++ getTypeComponents(bT.tpe)).distinct)

    val constrName = newTypeName(c.fresh("Impl"))

    val defaultConstr = DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(List()), 
      TypeTree(), 
      Block(List(
        Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())), 
      Literal(Constant(()))))

    val anonimpl = ClassDef(NoMods, constrName, Nil, Template(
      types.map(t => tq"$t"), 
      emptyValDef,
      defaultConstr +: memberDefs))

    c.Expr[A With B](q"""new With[$aT, $bT] { 
      def apply($a: $aT, $b: $bT) = {
        $anonimpl ;
        new $constrName
      }
    }""")
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
