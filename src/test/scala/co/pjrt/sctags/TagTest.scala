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

  def call(row: Int, cul: Int) =
    s"call cursor($row, $cul)"

  "Vim Tag line" - {
    "should produce a complete non-static tag line" in {
      val t = Tag(Some("Obj"), "tagName", false, 0, 1)

      val testFile = "TestFile.scala"
      t.vimTagLine(testFile) shouldBe
        s"""Obj.tagName\t$testFile\t${call(1, 2)}"\tlanguage:scala"""
    }

    "should produce a complete static tag line" in {
      val t = Tag(Some("Obj"), "tagName", true, 0, 1)

      val testFile = "TestFile.scala"
      t.vimTagLine(testFile) shouldBe
        s"""Obj.tagName\t$testFile\t${call(1, 2)}"\tfile:\tlanguage:scala"""
    }
  }
}
