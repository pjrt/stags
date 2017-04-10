package co.pjrt.stags

import org.scalatest.{FreeSpec, Matchers}

class ScopedTagTest extends FreeSpec with Matchers {

  "mkScopedTags should generate scoped tags" in {

    val scope = Seq("X", "Y", "Z")
    val tag = Tag("test", true, 0, 0)
    ScopedTag(scope, tag).mkScopedTags(3) shouldBe Seq(
        Tag("test", true, 0, 0),
        Tag("X.test", true, 0, 0),
        Tag("Y.X.test", true, 0, 0),
        Tag("Z.Y.X.test", true, 0, 0)
      )
  }

  "mkScopedTags should generate fewer scoped tags if asked" in {

    val scope = Seq("X", "Y", "Z")
    val tag = Tag("test", true, 0, 0)
    ScopedTag(scope, tag).mkScopedTags(2) shouldBe Seq(
        Tag("test", true, 0, 0),
        Tag("X.test", true, 0, 0),
        Tag("Y.X.test", true, 0, 0)
      )
  }

  "mkScopedTags should generate the full size scoped tags if asked for more than the limit" in {

    val scope = Seq("X", "Y", "Z")
    val tag = Tag("test", true, 0, 0)
    ScopedTag(scope, tag).mkScopedTags(4) shouldBe Seq(
        Tag("test", true, 0, 0),
        Tag("X.test", true, 0, 0),
        Tag("Y.X.test", true, 0, 0),
        Tag("Z.Y.X.test", true, 0, 0)
      )
  }

}
