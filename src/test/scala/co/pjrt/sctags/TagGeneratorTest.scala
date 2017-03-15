package co.pjrt.sctags

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

class TagGeneratorTest extends FreeSpec with Matchers {

  "Should generate unquantified tags for classes" in {
    val testFile =
      """
      |package co.pjrt.sctags.test
      |
      |class SomeClass {
      | def hello(name: String) = name
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags.size shouldBe 1
    tags.toSet shouldBe Set(
      Tag(None, "hello", Nil, TagPosition(4, 5))
    )
  }

  "Should generate quantified AND unquantified tags for objects" in {
    val testFile =
      """
      |object SomeObject {
      | def whatup(name: String) = name
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags.size shouldBe 2
    tags.toSet shouldBe Set(
      Tag(None, "whatup", Nil, TagPosition(2, 5)),
      Tag(Some("SomeObject"), "whatup", Nil, TagPosition(2, 5))
    )
  }

  "Should ONLY generate quantified tags for inner objects" in {
    val testFile =
      """
      |object SomeObject {
      | object InnerObject {
      |   def hello(name: String) = name
      | }
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags.size shouldBe 2
    tags.toSet shouldBe Set(
      Tag(None, "hello", Nil, TagPosition(3, 7)),
      Tag(Some("InnerObject"), "hello", Nil, TagPosition(3, 7))
    )
  }

}
