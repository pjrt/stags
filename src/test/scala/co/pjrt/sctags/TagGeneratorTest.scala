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
      | type Alias = String
      | type Undefined
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags.size shouldBe 4
    tags.toSet shouldBe Set(
      Tag(None, "SomeClass", Nil, TagPosition(3, 6)),
      Tag(None, "hello", Nil, TagPosition(4, 5)),
      Tag(None, "Alias", Nil, TagPosition(5, 6)),
      Tag(None, "Undefined", Nil, TagPosition(6, 6))
    )
  }

  "Should generate unquantified tags for traits" in {
    val testFile =
      """
      |package co.pjrt.sctags.test
      |
      |trait SomeTrait {
      | def hello(name: String) = name
      | type Alias = String
      | type Undefined
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags.size shouldBe 4
    tags.toSet shouldBe Set(
      Tag(None, "SomeTrait", Nil, TagPosition(3, 6)),
      Tag(None, "hello", Nil, TagPosition(4, 5)),
      Tag(None, "Alias", Nil, TagPosition(5, 6)),
      Tag(None, "Undefined", Nil, TagPosition(6, 6))
    )
  }

  "Should generate quantified AND unquantified tags for object members" in {
    val testFile =
      """
      |object SomeObject {
      | def whatup(name: String) = name
      | val userName, userName2 = "hello"
      | type Alias = Int
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags.size shouldBe 9
    tags.toSet shouldBe Set(
      Tag(None, "SomeObject", Nil, TagPosition(1, 7)),

      Tag(None, "whatup", Nil, TagPosition(2, 5)),
      Tag(None, "userName", Nil, TagPosition(3, 5)),
      Tag(None, "userName2", Nil, TagPosition(3, 15)),
      Tag(None, "Alias", Nil, TagPosition(4, 6)),

      Tag(Some("SomeObject"), "whatup", Nil, TagPosition(2, 5)),
      Tag(Some("SomeObject"), "userName", Nil, TagPosition(3, 5)),
      Tag(Some("SomeObject"), "userName2", Nil, TagPosition(3, 15)),
      Tag(Some("SomeObject"), "Alias", Nil, TagPosition(4, 6))
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

    tags.size shouldBe 4
    tags.toSet shouldBe Set(
      Tag(None, "SomeObject", Nil, TagPosition(1, 7)),
      Tag(None, "InnerObject", Nil, TagPosition(2, 8)),
      Tag(None, "hello", Nil, TagPosition(3, 7)),
      Tag(Some("InnerObject"), "hello", Nil, TagPosition(3, 7))
    )
  }

}
