package co.pjrt.sctags

import scala.meta._

case class Tag(
    prefix: Option[Term.Name],
    basicName: Term.Name,
    mods: Seq[Mod],
    pos: Position) {

  final val tokenName: String =
    prefix.fold(basicName.value)(_.value + "." + basicName.value)
}

case class TagPosition(line: Int, col: Int)
