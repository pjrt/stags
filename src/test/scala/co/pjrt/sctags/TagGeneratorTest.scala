package co.pjrt.sctags

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

import Utils._

class TagGeneratorTest extends FreeSpec with Matchers {

  "Should generate unqualified tags for classes" in {
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

    tags ~> List(
      Tag(None, "SomeClass", Nil, 3, 6),
      Tag(None, "hello", Nil, 4, 5),
      Tag(None, "Alias", Nil, 5, 6),
      Tag(None, "Undefined", Nil, 6, 6)
    )
  }

  "Should generate unqualified tags for traits" in {
    val testFile =
      """
      |package co.pjrt.sctags.test
      |
      |trait SomeTrait {
      | def hello(name: String) = name
      | type Alias = String
      | type Undefined <: SomeUpper
      | val defined1, defined2 = "hello"
      | val undefined: String
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~> List(
      Tag(None, "SomeTrait", Nil, 3, 6),
      Tag(None, "hello", Nil, 4, 5),
      Tag(None, "Alias", Nil, 5, 6),
      Tag(None, "Undefined", Nil, 6, 6),
      Tag(None, "defined1", Nil, 7, 5),
      Tag(None, "defined2", Nil, 7, 15),
      Tag(None, "undefined", Nil, 8, 5)
    )
  }

  "Should generate qualified AND unqualified tags for object members" in {
    val testFile =
      """
      |object SomeObject {
      | def whatup(name: String) = name
      | val userName, userName2 = "hello"
      | type Alias = Int
      | type Decl <: SomeUpper
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~> List(
      Tag(None, "SomeObject", Nil, 1, 7),
      Tag(None, "whatup", Nil, 2, 5),
      Tag(None, "userName", Nil, 3, 5),
      Tag(None, "userName2", Nil, 3, 15),
      Tag(None, "Alias", Nil, 4, 6),
      Tag(None, "Decl", Nil, 5, 6),
      Tag(Some("SomeObject"), "whatup", Nil, 2, 5),
      Tag(Some("SomeObject"), "userName", Nil, 3, 5),
      Tag(Some("SomeObject"), "userName2", Nil, 3, 15),
      Tag(Some("SomeObject"), "Alias", Nil, 4, 6),
      Tag(Some("SomeObject"), "Decl", Nil, 5, 6)
    )
  }

  "Should ONLY generate qualified tags for inner objects" in {
    val testFile =
      """
      |object SomeObject {
      | object InnerObject {
      |   def hello(name: String) = name
      | }
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~> List(
      Tag(None, "SomeObject", Nil, 1, 7),
      Tag(None, "InnerObject", Nil, 2, 8),
      Tag(Some("SomeObject"), "InnerObject", Nil, 2, 8),
      Tag(None, "hello", Nil, 3, 7),
      Tag(Some("InnerObject"), "hello", Nil, 3, 7)
    )
  }

  "Should generate qualified AND unqualified tags for package object members" in {
    val testFile =
      """
      |package co.pjrt.ctags
      |
      |package object test {
      | def whatup(name: String) = name
      | val userName, userName2 = "hello"
      | type Alias = Int
      | type Decl <: SomeUpper
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~> List(
      Tag(None, "test", Nil, 3, 15),
      Tag(None, "whatup", Nil, 4, 5),
      Tag(None, "userName", Nil, 5, 5),
      Tag(None, "userName2", Nil, 5, 15),
      Tag(None, "Alias", Nil, 6, 6),
      Tag(None, "Decl", Nil, 7, 6),
      Tag(Some("test"), "whatup", Nil, 4, 5),
      Tag(Some("test"), "userName", Nil, 5, 5),
      Tag(Some("test"), "userName2", Nil, 5, 15),
      Tag(Some("test"), "Alias", Nil, 6, 6),
      Tag(Some("test"), "Decl", Nil, 7, 6)
    )
  }

  "Should capture modifiers just fine" in {
    val testFile =
      """
      |package co.pjrt.ctags.test
      |
      |object SomeObject {
      | private[test] object InnerObject {
      |   private def privateHello(name: String) = name
      |   def publicHello(name: String) = name
      | }
      |}
      |sealed trait SealedTrait {
      |  final def f(name: String) = name
      |  protected[SomeObject] def protectedHello(name: String) = name
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    import Mod._
    import Name._
    tags ~>
      List(
        Tag(None, "SomeObject", Nil, 3, 7),
        Tag(
          None,
          "InnerObject",
          Seq(Private(Indeterminate("test"))),
          4, 22
        ),
        Tag(
          Some("SomeObject"),
          "InnerObject",
          Seq(Private(Indeterminate("test"))),
          4, 22
        ),
        Tag(
          None,
          "privateHello",
          Seq(Private(Anonymous())),
          5, 15
        ),
        Tag(
          Some("InnerObject"),
          "privateHello",
          Seq(Private(Anonymous())),
          5, 15
        ),
        Tag(None, "publicHello", Nil, 6, 7),
        Tag(Some("InnerObject"), "publicHello", Nil, 6, 7),
        Tag(None, "SealedTrait", Seq(Sealed()), 9, 13),
        Tag(None, "f", Seq(Final()), 10, 12),
        Tag(
          None,
          "protectedHello",
          Seq(Protected(Indeterminate("SomeObject"))),
          11, 28
        )
      )
  }
}
