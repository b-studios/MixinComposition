package de.unimarburg
package composition

import scala.reflect.api.Universe

// A slice of the scalac compiler
// can be implemented by a ToolBox or reflection
trait Compiler {

  val universe: Universe
  import universe._

  def fresh(name: String): String
  def parse(s: String): Tree
  def typecheck(t: Tree): Tree
  def untypecheck(t: Tree): Tree
}