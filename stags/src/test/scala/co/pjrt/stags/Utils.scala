package co.pjrt.stags

import scala.meta._

import org.scalatest.Matchers
import org.scalactic.source.Position

object Utils {

  import Matchers._

  implicit class SeqOfTagsOps(val testCode: String) extends AnyVal {

    def ~>(
        expected: Seq[(Scope, String, Boolean)]
      )(implicit pos: Position
      ): Unit = {
      testCode.parse[Source] match {
        case _: parsers.Parsed.Error => fail("Could not parse test code")
        case parsers.Parsed.Success(parsed) =>
          val actual = TagGenerator.generateTags(parsed)
          val a =
            actual.map(s => (s.scope, s.tag.tagName) -> s.tag.isStatic).toMap
          val b = expected.map(t => (t._1, t._2) -> t._3).toMap

          b.foreach {
            case (n, v) =>
              a.get(n).fold(fail(s"Expected $n, but not found")) { f =>
                if (f == v)
                  succeed
                else
                  fail(
                    s"Found $n:  isStatic = $f, but it was supposed to be not"
                  )
              }
          }
      }
    }
  }
}
