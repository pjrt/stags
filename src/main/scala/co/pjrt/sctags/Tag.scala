package co.pjrt.sctags

import scala.meta._

case class Tag(
    prefix: Option[String],
    basicName: String,
    mods: Seq[Mod],
    pos: TagPosition) {

  final val tagName: String =
    prefix.fold(basicName)(_ + "." + basicName)

  private final val tagAddress =
    pos.line + "G" + pos.col + "|"

  private final val term = ";\""
  private final val tab = "\t"

  private def extras(fields: Seq[(String, String)]) =
    term + tab + fields.map(t => t._1 + ":" + t._2).mkString(tab)

  /**
   * Visibility of the tag. This means either:
   *
   * * Private: Only seen in this file/object/class/trait
   * * PrivateIsh: Can be seen from more than just this file
   * * Public: Everything else
   *
   * Note that Protected also means Public. This visibility is only meant to
   * determine whether we create a static tag or not.
   *
   * This is not a complete implementation. We could technially do something
   * with protected[thisPackage] too.
   */
  lazy val visibility: Visibility =
    mods.collect {
      case Mod.Private(Name.Anonymous()) => Private
      case Mod.Private(Name.Indeterminate(name)) =>
        // DESNOTE(2017-03-21, prodriguez): If the name of the private thing
        // is the parent object, then it is just private.
        if (prefix.contains(name))
          Private
        else
          PrivateIsh
    }.headOption.getOrElse(Public)

  /**
   * Whether this is a static tag as defined in C
   */
  lazy val isStaticTag: Boolean =
    visibility == Private

  /**
   * Given a [[Tag]] and a file name, create a vim tag line
   *
   * See http://vimdoc.sourceforge.net/htmldoc/tagsrch.html#tags-file-format
   */
  def vimTagLine(fileName: String): String = {
    val static =
      if (isStaticTag) Seq(("file" -> fileName))
      else Seq.empty

    val langTag = "language" -> "scala"
    val fields = static :+ langTag
    List(tagName, fileName, tagAddress).mkString(tab) + extras(fields)
  }

  override def toString: String =
    s"Tag($tagName, $mods, ${pos.line}, ${pos.col})"
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

  implicit val ordering: Ordering[Tag] =
    Ordering.by(_.tagName)

}

/**
 * Visibility of a tag
 *
 * At the end of the day, we only care about whether we should make a static
 * tag or a non-static tag (private vs public). This is probably overkill.
 */
sealed trait Visibility
case object Private extends Visibility
case object PrivateIsh extends Visibility
case object Public extends Visibility

case class TagPosition(line: Int, col: Int)

object TagPosition {

  def fromPosition(pos: Position): TagPosition =
    TagPosition(pos.start.line, pos.start.column)
}
