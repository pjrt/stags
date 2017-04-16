package co.pjrt.stags

import scala.meta.Term

/**
 * The scope for a [[Tag]]
 */
sealed trait Scope {

  def addLocal(name: String): Scope

  final def addLocal(name: Term.Name): Scope = addLocal(name.value)

  final def localScope: LocalScope =
    this match {
      case t: LocalScope => t
      case PackageScope(_, l) => l
    }

  final def toSeq: Seq[String] =
    this match {
      case LocalScope(s) => s
      case PackageScope(s, l) => l.scope ++ s
    }
}

/**
 * The scope for the package itself
 */
final case class PackageScope(scope: Seq[String], local: LocalScope)
  extends Scope {

  def addLocal(n: String) = PackageScope(scope, local.addLocal(n))
}

/**
 * Scope for local stuff (objects)
 */
final case class LocalScope(scope: Seq[String]) extends Scope {
  def addLocal(name: String): LocalScope =
    LocalScope(name +: scope)

  final def contains(name: String): Boolean =
    scope.contains(name)
}

object LocalScope {
  final val empty: LocalScope = LocalScope(Seq.empty)
}
