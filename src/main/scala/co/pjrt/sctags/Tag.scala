package co.pjrt.sctags

import scala.meta._

case class Tag(
    prefix: Option[String],
    basicName: String,
    mods: Seq[Mod],
    pos: TagPosition
) {

  final val tokenName: String =
    prefix.fold(basicName)(_ + "." + basicName)

  override def toString: String =
    s"Tag($prefix, $basicName, $mods, ${pos.line}, ${pos.col})"
}

object Tag {

  def apply(
      prefix: Option[Name],
      basicName: Name,
      mods: Seq[Mod],
      pos: Position
    ): Tag = {

    Tag(
      prefix.map(_.value),
      basicName.value,
      mods,
      TagPosition.fromPosition(pos)
    )
  }

  def apply(
      prefix: Option[String],
      basicName: String,
      mods: Seq[Mod],
      pos: Position
    ): Tag = {

    Tag(prefix, basicName, mods, TagPosition.fromPosition(pos))
  }

}

case class TagPosition(line: Int, col: Int)

object TagPosition {

  def fromPosition(pos: Position): TagPosition =
    TagPosition(pos.start.line, pos.start.column)
}
