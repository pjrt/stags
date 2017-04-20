package co.pjrt.stags

import scala.meta.Term

/**
 * The scope for a [[Tag]]
 */
final case class Scope(packageScope: Seq[String], localScope: Seq[String]) {

  final def addLocal(name: String): Scope =
    Scope(packageScope, name +: localScope)

  final def addLocal(name: Term.Name): Scope = addLocal(name.value)

  final def toSeq: Seq[String] = packageScope ++ localScope

  final def localContains(name: String): Boolean =
    localScope.contains(name)

}

object Scope {

  final def apply(packageScope: Seq[String]): Scope =
    Scope(packageScope, Seq.empty)

  final val empty: Scope = Scope(Seq.empty, Seq.empty)
}
