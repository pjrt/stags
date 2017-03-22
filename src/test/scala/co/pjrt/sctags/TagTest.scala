package co.pjrt.sctags

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

class TagTest extends FreeSpec with Matchers {

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

  "Static tags" - {

    "Are tags that are private" in {
      val mods = Seq(
        priv(None)
      )
      val t = Tag(Some("Obj"), "tagName", mods, 0,0)
      t.isStaticTag shouldBe true
    }

    "Are tags that are private and have the scope as the parent" in {
      val mods = Seq(
        priv(Some("Obj"))
      )
      val t = Tag(Some("Obj"), "tagName", mods, 0,0)
      t.isStaticTag shouldBe true
    }

    "Are NOT private that don't have the scope as the parent" in {
      val mods = Seq(
        priv(Some("test"))
      )
      val t = Tag(Some("Obj"), "tagName", mods, 0,0)
      t.isStaticTag shouldBe false
    }

    "Are NOT any other visibility" in {
      val prodMods1 = Seq(prod(None))
      val prodMods2 = Seq(prod(Some("Obj")))
      val publicMods = Seq.empty[Mod]

      val t1 = Tag(Some("Obj"), "tagName", prodMods1, 0,0)
      val t2 = Tag(Some("Obj"), "tagName", prodMods2, 0,0)
      val t3 = Tag(Some("Obj"), "tagName", publicMods, 0,0)
      t1.isStaticTag shouldBe false
      t2.isStaticTag shouldBe false
      t3.isStaticTag shouldBe false
    }
  }

  "Vim Tag line" - {
    "should produce a complete non-static tag line" in {
      val t = Tag(Some("Obj"), "tagName", Nil, 0, 1)

      val testFile = "TestFile.scala"
      t.vimTagLine(testFile) shouldBe
        s"""Obj.tagName\t$testFile\t0G1|;"\tlanguage:scala"""
    }

    "should produce a complete static tag line" in {
      val t = Tag(Some("Obj"), "tagName", Seq(priv(None)), 0, 1)

      val testFile = "TestFile.scala"
      t.vimTagLine(testFile) shouldBe
        s"""Obj.tagName\t$testFile\t0G1|;"\tfile:$testFile\tlanguage:scala"""
    }
  }
}
