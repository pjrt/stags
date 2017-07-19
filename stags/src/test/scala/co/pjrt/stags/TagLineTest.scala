package co.pjrt.stags

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

import co.pjrt.stags.paths.Path

class TagLineTest extends FreeSpec with Matchers {

  import Mod._
  import Name._

  def priv(scope: Option[String]) =
    scope match {
      case None    => Private(Anonymous())
      case Some(n) => Private(Indeterminate(n))
    }

  def prod(scope: Option[String]) =
    scope match {
      case None    => Protected(Anonymous())
      case Some(n) => Protected(Indeterminate(n))
    }

  private def call(row: Int, cul: Int) =
    s"call cursor($row, $cul)"

  "Vim Tag line" - {
    "should produce a complete non-static tag line" in {
      val t = Tag("tagName", false, 0, 1)

      val testFile = Path.fromString("TestFile.scala")
      TagLine(t, testFile).vimTagLine shouldBe
        s"""tagName\t$testFile\t${call(1, 2)}"\tlanguage:scala"""
    }

    "should produce a complete static tag line" in {
      val t = Tag("tagName", true, 0, 1)

      val testFile = Path.fromString("TestFile.scala")
      TagLine(t, testFile).vimTagLine shouldBe
        s"""tagName\t$testFile\t${call(1, 2)}"\tfile:\tlanguage:scala"""
    }
  }

  val pwd = System.getProperty("user.dir")

  def abs(p: String): String =
    pwd + "/" + p

  "relativize" - {

    def testRelative(targetS: String, filePathS: String, expectedS: String) = {

      val target = Path.fromString(targetS)
      val filePath = Path.fromString(filePathS)
      val tag = Tag("tagName", false, 0, 1)

      TagLine(tag, filePath)
        .relativize(target)
        .filePath
        .toString shouldBe expectedS

    }

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
          abs(".git/tag"),
          abs("src/main/stuff/hello.scala"),
          "../src/main/stuff/hello.scala"
        )
      }

      "relative: branch: mixed: relative tag" in {
        testRelative(
          ".git/tag",
          abs("src/main/stuff/hello.scala"),
          "../src/main/stuff/hello.scala"
        )
      }

      "absolute: branch: mixed: absolute tag" in {
        testRelative(
          abs(".git/tag"),
          "src/main/stuff/hello.scala",
          "../src/main/stuff/hello.scala"
        )
      }

      "relative: child" in {
        testRelative(
          "src/tags",
          "src/main/stuff/hello.scala",
          "main/stuff/hello.scala"
        )
      }

      "absolute: child" in {
        testRelative(
          abs("src/tags"),
          abs("src/main/stuff/hello.scala"),
          "main/stuff/hello.scala"
        )
      }

      "relative: parent" in {
        testRelative(
          "../tags",
          "src/main/stuff/hello.scala",
          "stags/src/main/stuff/hello.scala"
        )
      }

      "absolute: parent" in {
        testRelative(
          abs("../tags"),
          abs("src/main/stuff/hello.scala"),
          "stags/src/main/stuff/hello.scala"
        )
      }

      "relative: pwd" in {
        testRelative(
          "tags",
          "src/main/stuff/hello.scala",
          "src/main/stuff/hello.scala"
        )
      }

      "absolute: pwd" in {
        testRelative(
          abs("tags"),
          abs("src/main/stuff/hello.scala"),
          "src/main/stuff/hello.scala"
        )
      }

      "relative: pwd: mixed: relative tag" in {
        testRelative(
          "tags",
          abs("src/main/stuff/hello.scala"),
          "src/main/stuff/hello.scala"
        )
      }

      "absolute: pwd: mixed: absolute tag file" in {
        testRelative(
          abs("tags"),
          "src/main/stuff/hello.scala",
          "src/main/stuff/hello.scala"
        )
      }
    }
  }

  def mkTagFromIdent(ident: String): TagLine = {
    val t = Tag(ident, false, 0, 1)

    val testFile = Path.fromString("TestFile.scala")
    TagLine(t, testFile)
  }
  "ordering" - {
    "foldcase" - {
      "the order should be case insensitive" in {
        val t1 = mkTagFromIdent("abcde2")
        val t2 = mkTagFromIdent("aBcdE1")
        val t3 = mkTagFromIdent("ABCD")
        val res = TagLine.foldCaseSorting(Seq(t1, t2, t3))
        res shouldBe (Seq(t3, t2, t1))
      }

      "_ (underscore) should be after alphanum chars" in {
        val t1 = mkTagFromIdent("_abc")
        val t2 = mkTagFromIdent("abc_de")
        val t3 = mkTagFromIdent("abc123_de")
        val res = TagLine.foldCaseSorting(Seq(t1, t2, t3))
        res shouldBe (Seq(t3, t2, t1))
      }
    }
  }
}
