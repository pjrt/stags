package co.pjrt.stags

import org.scalatest.{FreeSpec, Matchers}

class ScopedTagTest extends FreeSpec with Matchers {

  def lscope(strs: String*) = Scope(strs.toSeq)

  private val addr = "some-addr"

  "mkScopedTags should generate scoped tags" in {

    val scope = lscope("X", "Y", "Z")
    val tag = Tag("test", true, addr)
    ScopedTag(scope, tag).mkScopedTags(3) shouldBe Seq(
      Tag("test", true, addr),
      Tag("X.test", true, addr),
      Tag("Y.X.test", true, addr),
      Tag("Z.Y.X.test", true, addr)
    )
  }

  "mkScopedTags should generate fewer scoped tags if asked" in {

    val scope = lscope("X", "Y", "Z")
    val tag = Tag("test", true, addr)
    ScopedTag(scope, tag).mkScopedTags(2) shouldBe Seq(
      Tag("test", true, addr),
      Tag("X.test", true, addr),
      Tag("Y.X.test", true, addr)
    )
  }

  "mkScopedTags should generate the full size scoped tags if asked for more than the limit" in {

    val scope = lscope("X", "Y", "Z")
    val tag = Tag("test", true, addr)
    ScopedTag(scope, tag).mkScopedTags(4) shouldBe Seq(
      Tag("test", true, addr),
      Tag("X.test", true, addr),
      Tag("Y.X.test", true, addr),
      Tag("Z.Y.X.test", true, addr)
    )
  }

}
