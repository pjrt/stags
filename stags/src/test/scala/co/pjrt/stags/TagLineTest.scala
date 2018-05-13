package co.pjrt.stags

import java.nio.file._

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

import co.pjrt.stags.paths.AbsolutePath

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

  private[this] val userDir = System.getProperty("user.dir")
  private val cwd = AbsolutePath.forceAbsolute(Paths.get(userDir))

  "Vim Tag line" - {
    "should produce a complete non-static tag line" in {
      val addr = "val \\zstagName = 2"
      val t = Tag("tagName", false, addr, "k")

      val testFile = AbsolutePath.fromPath(cwd, Paths.get("TestFile.scala"))
      TagLine(t, testFile.path).vimTagLine shouldBe
        s"""tagName\t$testFile\t${addr};"\tk"""
    }

    "should produce a complete static tag line" in {
      val addr = "val \\zstagName = 2"
      val t = Tag("tagName", true, addr, "k")

      val testFile = AbsolutePath.fromPath(cwd, Paths.get("TestFile.scala"))
      TagLine(t, testFile.path).vimTagLine shouldBe
        s"""tagName\t$testFile\t${addr};"\tk\tfile:"""
    }
  }

  def mkTagFromIdent(ident: String): TagLine = {
    val t = Tag(ident, false, "some-addr", "k")

    val testFile = Paths.get("TestFile.scala")
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
