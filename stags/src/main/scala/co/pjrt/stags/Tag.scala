package co.pjrt.stags

import java.nio.file.Path

import scala.util.Sorting

/**
 * A [[ScopedTag]] is a [[Tag]] along with a scope that determines the
 * qualification of the [[Tag]]
 */
final case class ScopedTag(scope: Scope, tag: Tag) {

  final def mkScopedTags(limit: Int): Seq[Tag] =
    scope.toSeq
      .foldLeft((Seq(tag), tag.tagName, limit)) {
        case (acc @ (_, _, l), _) if l <= 0 => acc
        case ((tags, acc, l), x) =>
          val newTag = tag.copy(tagName = x + "." + acc)
          (tags :+ newTag, newTag.tagName, l - 1)
      }
      ._1

  final def mkTagLines(path: Path, limit: Int): Seq[TagLine] =
    mkScopedTags(limit).map(TagLine(_, path.toString))
}

object ScopedTag {

  def apply(
      scope: Scope,
      tokenName: String,
      isStatic: Boolean,
      tagAddress: String,
      kind: String
    ): ScopedTag = {

    ScopedTag(scope, Tag(tokenName, isStatic, tagAddress, kind))
  }
}

/**
 * A [[Tag]] contains all the information necessary to create a tag line from
 * a token in the syntax tree
 *
 * It does not contain the filename since that's information that exists
 * outside of the syntax tree.
 */
final case class Tag(
    tagName: String,
    isStatic: Boolean,
    tagAddress: String,
    kind: String) {

  override def toString: String =
    s"Tag($tagName, ${if (isStatic) "static" else "non-static"}, $tagAddress, $kind)"
}

/**
 * A [[TagLine]] is simple a [[Tag]] and a Path
 *
 * It represents the final, file-representation of a tag.
 */
final case class TagLine(tag: Tag, filePath: String) {

  private final val term = ";\""
  private final val tab = "\t"

  private def extras(fields: List[(String, String)]) =
    term + tab + (tag.kind :: fields.map(t => t._1 + ":" + t._2)).mkString(tab)

  /**
   * Given a [[Tag]] and a file name, create a vim tag line
   *
   * See http://vimdoc.sourceforge.net/htmldoc/tagsrch.html#tags-file-format
   */
  final val vimTagLine: String = {
    import tag._

    val static = if (isStatic) Some("file" -> "") else None

    val fields = (static :: Nil).flatten
    List(tagName, filePath, tagAddress).mkString(tab) + extras(fields)
  }
}

object TagLine {

  /**
   * Sorts the list of taglines in fold-case ordering (ie: case insensitive)
   *
   * This is the preferred way of sorting tags in vim since it avoids linear
   * search when `ignorecase` is set. See `:h tags-file-format`
   */
  final def foldCaseSorting(tags: Seq[TagLine]): Seq[TagLine] = {

    tags.sortBy(_.tag.tagName.toUpperCase)
  }

  /**
   * Sorts the list of taglines in ascii ordering (ie: case sensitive)
   */
  final def asciiSorting(tags: Seq[TagLine]): Seq[TagLine] = {
    implicit val sorting: Ordering[TagLine] = Ordering.by(_.tag.tagName)

    Sorting.stableSort(tags)
  }
}
