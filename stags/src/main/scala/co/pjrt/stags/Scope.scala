package co.pjrt.stags

import scala.meta.Term

/**
 * The scope for a [[Tag]]
 */
final case class Scope(packageScope: Seq[String], localScope: Seq[String]) {

  def addLocal(name: String): Scope =
    Scope(packageScope, name +: localScope)

  def addLocal(name: Term.Name): Scope = addLocal(name.value)

  def toSeq: Seq[String] = localScope ++ packageScope

  def localContains(name: String): Boolean =
    localScope.contains(name)
}

object Scope {

  final def apply(packageScope: Seq[String]): Scope =
    Scope(packageScope, Seq.empty)

  final val empty: Scope = Scope(Seq.empty, Seq.empty)
}
