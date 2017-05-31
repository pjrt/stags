package co.pjrt.stags

import scala.meta._

import org.scalatest.Matchers
import org.scalactic.source.Position

object Utils {

  import Matchers._

  /**
   * Compare the two set of tags, while smartly displaying what went wrong
   */
  def compareTags(
      actual: Seq[ScopedTag],
      expected: Seq[ScopedTag]
    )(implicit limit: Int,
      pos: Position
    ): Unit = {

    def toMap(s: Seq[ScopedTag]): Map[String, (Boolean, TagPosition)] =
      s.flatMap(
          t =>
            t.mkScopedTags(limit)
              .map(
                _.tagName -> ((t.tag.isStatic, t.tag.row -> t.tag.column))
            )
        )
        .toMap
    val aSize = actual.size
    val eSize = expected.size
    if (aSize > eSize)
      fail(s"Got more tags than expected $aSize > $eSize")
    else if (aSize < eSize)
      fail(s"Got less tags than expected $aSize < $eSize")
    else ()

    val mActual: Map[String, (Boolean, TagPosition)] = toMap(actual)

    def testContent(
        name: String,
        actual: (Boolean, TagPosition),
        expected: (Boolean, TagPosition)
      ) = {
      if (expected == actual) ()
      else
        fail(
          s"Found tag `$name` but $actual /= $expected"
        )
    }

    toMap(expected).foreach {
      case (k, v) =>
        mActual
          .get(k)
          .map { c =>
            testContent(k, c, v)
          }
          .getOrElse(fail(s"Did not find `$k`"))
    }
  }

  implicit class SeqOfTagsOps(val testCode: String) extends AnyVal {

    def ~>(
        expected: Seq[ScopedTag]
      )(implicit limit: Int,
        pos: Position
      ): Unit = {
      testCode.parse[Source] match {
        case _: parsers.Parsed.Error => fail("Could not parse test code")
        case parsers.Parsed.Success(parsed) =>
          val actual = TagGenerator.generateTags(parsed)
          compareTags(actual, expected)
      }
    }
  }
}
