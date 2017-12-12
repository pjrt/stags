package co.pjrt.stags

import scala.meta._
import scala.util.Sorting

import co.pjrt.stags.paths.Path

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
    mkScopedTags(limit).map(TagLine(_, path))
}

object ScopedTag {

  def fromMember(scope: Scope, isStatic: Boolean, member: Member): ScopedTag = {

    val tokenName = member.name.toString
    val nameS = member.name.pos.start

    // DESNOTE(2017-12-11, pjrt): Remove any annotations.
    val tokens = member.tokens
    val linePostAnnot =
      if (tokens.head.is[Token.At])
        tokens.dropWhile(!_.is[Token.LF]).tail
      else
        tokens

    // DESNOTE(2017-12-11, pjrt): Take the first line of the field, avoiding
    // whole bodies
    val line = linePostAnnot.takeWhile(!_.is[Token.LF])

    val nameOffset = nameS - line.head.start
    val tagAddress = {
      val b = new StringBuilder(line.mkString)
      b.insert(0, '/')
      b.insert(nameOffset + 1, "\\zs")
      b.append('/')
    }

    ScopedTag(scope, Tag(tokenName, isStatic, tagAddress.mkString))
  }
}

/**
 * A [[Tag]] contains all the information necessary to create a tag line from
 * a token in the syntax tree
 *
 * It does not contain the filename since that's information that exists
 * outside of the syntax tree.
 */
final case class Tag(tagName: String, isStatic: Boolean, tagAddress: String) {

  override def toString: String =
    s"Tag($tagName, ${if (isStatic) "static" else "non-static"}, $tagAddress)"
}

/**
 * A [[TagLine]] is simple a [[Tag]] and a [[co.pjrt.stags.paths.Path]]
 *
 * It represents the final, file-representation of a tag.
 */
final case class TagLine(tag: Tag, filePath: Path) {

  private final val term = ";\""
  private final val tab = "\t"

  private def extras(fields: Seq[(String, String)]) =
    term + tab + fields.map(t => t._1 + ":" + t._2).mkString(tab)

  /**
   * Given a [[Tag]] and a file name, create a vim tag line
   *
   * See http://vimdoc.sourceforge.net/htmldoc/tagsrch.html#tags-file-format
   */
  final val vimTagLine: String = {
    import tag._

    val static =
      if (isStatic) Seq(("file" -> ""))
      else Seq.empty

    val langTag = "language" -> "scala"
    val fields = static :+ langTag
    List(tagName, filePath.toString, tagAddress).mkString(tab) + extras(fields)
  }

  /**
   * Modify the [[filePath]] to be relative to the given [[co.pjrt.stags.paths.Path]]
   */
  final def relativize(outputPath: Path): TagLine = {
    // DESNOTE(2017-04-04, pjrt) Due to the way `Paths.relativize` works, we
    // need to get the parent of the output file.
    TagLine(tag, outputPath.getParent.relativize(filePath))
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
    implicit val sorting: Ordering[TagLine] =
      Ordering.fromLessThan(
        (x, y) => x.tag.tagName.toUpperCase < y.tag.tagName.toUpperCase
      )

    Sorting.stableSort(tags)(implicitly, sorting)
  }

  /**
   * Sorts the list of taglines in ascii ordering (ie: case sensitive)
   */
  final def asciiSorting(tags: Seq[TagLine]): Seq[TagLine] = {
    implicit val sorting: Ordering[TagLine] = Ordering.by(_.tag.tagName)

    Sorting.stableSort(tags)
  }
}
