package co.pjrt.stags

import java.nio.file.Paths

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

class TagLineTest extends FreeSpec with Matchers {

  import Mod._
  import Name._

  def priv(scope: Option[String]) =
    scope match {
      case None => Private(Anonymous())
      case Some(n) => Private(Indeterminate(n))
    }

  def prod(scope: Option[String]) =
    scope match {
      case None => Protected(Anonymous())
      case Some(n) => Protected(Indeterminate(n))
    }

  private def call(row: Int, cul: Int) =
    s"call cursor($row, $cul)"

  "Vim Tag line" - {
    "should produce a complete non-static tag line" in {
      val t = Tag(Some("Obj"), "tagName", false, 0, 1)

      val testFile = Paths.get("TestFile.scala")
      TagLine(t, testFile).vimTagLine shouldBe
        s"""Obj.tagName\t$testFile\t${call(1, 2)}"\tlanguage:scala"""
    }

    "should produce a complete static tag line" in {
      val t = Tag(Some("Obj"), "tagName", true, 0, 1)

      val testFile = Paths.get("TestFile.scala")
      TagLine(t, testFile).vimTagLine shouldBe
        s"""Obj.tagName\t$testFile\t${call(1, 2)}"\tfile:\tlanguage:scala"""
    }
  }

  "relativize" - {

    def testRelative(targetS: String, filePathS: String, expectedS: String) = {

      val target = Paths.get(targetS)
      val filePath = Paths.get(filePathS)
      val tag = Tag(None, "tagName", false, 0, 1)

      val expected = Paths.get(expectedS)

      TagLine(tag, filePath).relativize(target).filePath shouldBe expected

    }

    // TODO: pjrt Add tests for self, children and parents
    // This currently doesn't work
    "should modify the filepath to be relative from the target in" - {
      "relative: branch" in {
        testRelative(
          ".git/tag",
          "src/main/stuff/hello.scala",
          "../src/main/stuff/hello.scala"
        )
      }

      "absolute: branch" in {
        testRelative(
          "/home/code/.git/tag",
          "/home/code/src/main/stuff/hello.scala",
          "../src/main/stuff/hello.scala"
        )
      }
    }
  }
}
