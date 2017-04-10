package co.pjrt.stags

import org.scalatest.Matchers

object Utils {

  import Matchers._

  /**
   * Compare the two set of tags, while smartly displaying what went wrong
   */
  def compareTags(actual: Seq[ScopedTag], expected: Seq[ScopedTag])(implicit limit: Int): Unit = {

    def toMap(s: Seq[ScopedTag]) =
      s.map(t => t.mkScopedTags(limit) -> ((t.tag.isStatic, t.tag.row -> t.tag.column))).toMap
    val aSize = actual.size
    val eSize = expected.size
    if (aSize > eSize)
      fail(s"Got more tags than expected $aSize > $eSize")
    else if (aSize < eSize)
      fail(s"Got less tags than expected $aSize < $eSize")
    else ()
    val mActual: Map[Seq[Tag], (Boolean, TagPosition)] =
      toMap(actual)

    def testContent(t: (Boolean, TagPosition), t2: Tag) = {
      val expectedContent = (t2.isStatic, t2.pos)
      val actualContent = (t._1, t._2)
      if (expectedContent == actualContent) ()
      else
        fail(
          s"Found tag `${t2.tagName}` but $actualContent /= $expectedContent"
        )
    }

    expected.foreach { e =>
      mActual
        .get(e.mkScopedTags(limit))
        .map { c =>
          testContent(c, e.tag)
        }
        .getOrElse(fail(s"Did not find `${e.mkScopedTags(limit)}`"))
    }
  }

  implicit class SeqOfTagsOps(val actual: Seq[ScopedTag]) extends AnyVal {

    def ~>(expected: Seq[ScopedTag])(implicit limit: Int = 1): Unit =
      compareTags(actual, expected)(limit)
  }
}
