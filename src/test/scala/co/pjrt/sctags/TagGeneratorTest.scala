package co.pjrt.sctags

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

class TagGeneratorTest extends FreeSpec with Matchers {

  val testFile =
    """
    |package co.pjrt.sctags.test
    |
    |class SomeClass {
    | def hello(name: String) = name
    |}
    |
    |object SomeObject {
    | def whatup(name: String) = name
    |}
    """.stripMargin

  "Should generate some tags" in {
    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags.size shouldBe 3
    val input = Input.String(testFile)
    tags.toSet shouldBe Set(
      Tag(None, "hello", Nil, Position.Range(input, 49, 79)),
      Tag(None, "whatup", Nil, Position.Range(input, 104, 135)),
      Tag(Some("SomeObject"), "whatup", Nil, Position.Range(input, 104, 135))
    )
  }

}
