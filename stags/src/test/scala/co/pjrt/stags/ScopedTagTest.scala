package co.pjrt.stags

import org.scalatest.{Tag as _, *}
import org.scalatest.matchers.*

class ScopedTagTest extends freespec.AnyFreeSpec with should.Matchers {

  def lscope(strs: String*) = Scope(strs.toSeq)

  private val addr = "some-addr"

  "mkScopedTags should generate scoped tags" in {

    val scope = lscope("X", "Y", "Z")
    val tag = Tag("test", true, addr, "k")
    ScopedTag(scope, tag).mkScopedTags(3) should be(
      Seq(
        Tag("test", true, addr, "k"),
        Tag("X.test", true, addr, "k"),
        Tag("Y.X.test", true, addr, "k"),
        Tag("Z.Y.X.test", true, addr, "k")
      )
    )
  }

  "mkScopedTags should generate fewer scoped tags if asked" in {

    val scope = lscope("X", "Y", "Z")
    val tag = Tag("test", true, addr, "k")
    ScopedTag(scope, tag).mkScopedTags(2) should be(
      Seq(
        Tag("test", true, addr, "k"),
        Tag("X.test", true, addr, "k"),
        Tag("Y.X.test", true, addr, "k")
      )
    )
  }

  "mkScopedTags should generate the full size scoped tags if asked for more than the limit" in {

    val scope = lscope("X", "Y", "Z")
    val tag = Tag("test", true, addr, "k")
    ScopedTag(scope, tag).mkScopedTags(4) should be(
      Seq(
        Tag("test", true, addr, "k"),
        Tag("X.test", true, addr, "k"),
        Tag("Y.X.test", true, addr, "k"),
        Tag("Z.Y.X.test", true, addr, "k")
      )
    )
  }

}
