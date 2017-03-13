package co.pjrt.sctags

import scala.meta._

case class Tag(
    prefix: Option[String],
    basicName: String,
    mods: Seq[Mod],
    pos: Position
) {

  final val tokenName: String =
    prefix.fold(basicName)(_ + "." + basicName)

  override def toString: String =
    s"Tag($prefix, $basicName, $mods)"
}

object Tag {
  def apply(
      prefix: Option[Term.Name],
      basicName: Term.Name,
      mods: Seq[Mod],
      pos: Position
    ): Tag = Tag(prefix.map(_.value), basicName.value, mods, pos)
}

case class TagPosition(line: Int, col: Int)
