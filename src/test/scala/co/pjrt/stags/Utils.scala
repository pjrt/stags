package co.pjrt.stags

import org.scalatest.Matchers

object Utils {

  import Matchers._

  private def toMap(s: Seq[Tag]) =
    s.map(t => t.tagName -> ((t.isStatic, t.row -> t.column))).toMap

  /**
   * Compare the two set of tags, while smartly displaying what went wrong
   */
  def compareTags(actual: Seq[Tag], expected: Seq[Tag]): Unit = {

    val aSize = actual.size
    val eSize = expected.size
    if (aSize > eSize)
      fail(s"Got more tags than expected $aSize > $eSize")
    else if (aSize < eSize)
      fail(s"Got less tags than expected $aSize < $eSize")
    else ()
    val mActual: Map[String, (Boolean, TagPosition)] =
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
        .get(e.tagName)
        .map { c =>
          testContent(c, e)
        }
        .getOrElse(fail(s"Did not find `${e.tagName}`"))
    }
  }

  implicit class SeqOfTagsOps(val actual: Seq[Tag]) extends AnyVal {

    def ~>(expected: Seq[Tag]): Unit =
      compareTags(actual, expected)
  }
}
