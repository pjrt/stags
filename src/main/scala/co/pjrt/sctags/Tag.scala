package co.pjrt.sctags

import scala.meta._

/**
 * A [[Tag]] contains all the information necessary to create a tag line from
 * a token in the syntax tree
 *
 * It does not contain the filename since that's information that exists
 * outside of the syntax tree.
 */
case class Tag(
    prefix: Option[String],
    basicName: String,
    isStatic: Boolean,
    row: Int,
    column: Int) {

  final val tagName: String =
    prefix.fold(basicName)(_ + "." + basicName)

  lazy val pos: TagPosition = row -> column

  private final val tagAddress =
    row + "G" + column + "|"

  private final val term = ";\""
  private final val tab = "\t"

  private def extras(fields: Seq[(String, String)]) =
    term + tab + fields.map(t => t._1 + ":" + t._2).mkString(tab)

  /**
   * Given a [[Tag]] and a file name, create a vim tag line
   *
   * See http://vimdoc.sourceforge.net/htmldoc/tagsrch.html#tags-file-format
   */
  def vimTagLine(fileName: String): String = {
    val static =
      if (isStatic) Seq(("file" -> fileName))
      else Seq.empty

    val langTag = "language" -> "scala"
    val fields = static :+ langTag
    List(tagName, fileName, tagAddress).mkString(tab) + extras(fields)
  }

  override def toString: String =
    s"Tag($tagName, ${if (isStatic) "static" else "non-static"}, $row, $column)"
}

object Tag {

  def apply(
      prefix: Option[Name],
      basicName: Name,
      isStatic: Boolean,
      pos: Position
    ): Tag = {

    val prefix2 = prefix.map(_.value)
    Tag(
      prefix2,
      basicName.value,
      isStatic,
      pos.start.line,
      pos.start.column
    )
  }

  def apply(
      prefix: Option[String],
      basicName: String,
      isStatic: Boolean,
      pos: Position
    ): Tag = {

    Tag(
      prefix,
      basicName,
      isStatic,
      pos.start.line,
      pos.start.column
    )
  }

  implicit val ordering: Ordering[Tag] =
    Ordering.by(_.tagName)
}
