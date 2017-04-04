package co.pjrt.stags

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

import Utils._

class TagGeneratorTest extends FreeSpec with Matchers {

  "Should generate unqualified tags for classes" in {
    val testFile =
      """
      |package co.pjrt.stags.test
      |
      |class SomeClass() {
      | def hello(name: String) = name
      | type Alias = String
      | type Undefined
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~> List(
      Tag(None, "SomeClass", false, 3, 6),
      Tag(None, "hello", false, 4, 5),
      Tag(None, "Alias", false, 5, 6),
      Tag(None, "Undefined", false, 6, 6)
    )
  }

  "Should generate unqualified tags for traits" in {
    val testFile =
      """
      |package co.pjrt.stags.test
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
      Tag(None, "SomeTrait", false, 3, 6),
      Tag(None, "hello", false, 4, 5),
      Tag(None, "Alias", false, 5, 6),
      Tag(None, "Undefined", false, 6, 6),
      Tag(None, "defined1", false, 7, 5),
      Tag(None, "defined2", false, 7, 15),
      Tag(None, "undefined", false, 8, 5)
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
      | val (tUserName, tUserName2: String) = ("hello1", "hello2")
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~> List(
      Tag(None, "SomeObject", false, 1, 7),
      Tag(None, "whatup", false, 2, 5),
      Tag(None, "userName", false, 3, 5),
      Tag(None, "userName2", false, 3, 15),
      Tag(None, "Alias", false, 4, 6),
      Tag(None, "Decl", false, 5, 6),
      Tag(None, "tUserName", false, 6, 6),
      Tag(None, "tUserName2", false, 6, 17),
      Tag(Some("SomeObject"), "whatup", false, 2, 5),
      Tag(Some("SomeObject"), "userName", false, 3, 5),
      Tag(Some("SomeObject"), "userName2", false, 3, 15),
      Tag(Some("SomeObject"), "Alias", false, 4, 6),
      Tag(Some("SomeObject"), "Decl", false, 5, 6),
      Tag(Some("SomeObject"), "tUserName", false, 6, 6),
      Tag(Some("SomeObject"), "tUserName2", false, 6, 17)
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
      Tag(None, "SomeObject", false, 1, 7),
      Tag(None, "InnerObject", false, 2, 8),
      Tag(Some("SomeObject"), "InnerObject", false, 2, 8),
      Tag(None, "hello", false, 3, 7),
      Tag(Some("InnerObject"), "hello", false, 3, 7)
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
      Tag(None, "test", false, 3, 15),
      Tag(None, "whatup", false, 4, 5),
      Tag(None, "userName", false, 5, 5),
      Tag(None, "userName2", false, 5, 15),
      Tag(None, "Alias", false, 6, 6),
      Tag(None, "Decl", false, 7, 6),
      Tag(Some("test"), "whatup", false, 4, 5),
      Tag(Some("test"), "userName", false, 5, 5),
      Tag(Some("test"), "userName2", false, 5, 15),
      Tag(Some("test"), "Alias", false, 6, 6),
      Tag(Some("test"), "Decl", false, 7, 6)
    )
  }

  "Should capture modifiers and generate the correct isStatic" in {
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
      |  protected[test] def protectedHello(name: String) = name
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~>
      List(
        Tag(None, "SomeObject", false, 3, 7),
        Tag(
          None,
          "InnerObject",
          false,
          4,
          22
        ),
        Tag(
          Some("SomeObject"),
          "InnerObject",
          false,
          4,
          22
        ),
        Tag(
          None,
          "privateHello",
          true,
          5,
          15
        ),
        Tag(
          Some("InnerObject"),
          "privateHello",
          true,
          5,
          15
        ),
        Tag(None, "publicHello", false, 6, 7),
        Tag(Some("InnerObject"), "publicHello", false, 6, 7),
        Tag(None, "SealedTrait", false, 9, 13),
        Tag(None, "f", false, 10, 12),
        Tag(
          None,
          "protectedHello",
          false,
          11,
          22
        )
      )
  }

  "Should generate tags for Ctor params" - {
    "for classes" in {
      val testFileClass =
        s"""
        |class SomeClass(
        |   name: String,
        |   val number: Int,
        |   private val age: Int
        |)
        """.stripMargin

      val classTags =
        TagGenerator.generateTags(testFileClass.parse[Source].get)

      classTags ~> Seq(
        Tag(None, "SomeClass", false, 1, 6),
        Tag(None, "name", true, 2, 3),
        Tag(None, "number", false, 3, 7),
        Tag(None, "age", true, 4, 15)
      )
    }

    "for case classes" in {
      val testFileCase =
        s"""
        |case class SomeClass(
        |   name: String,
        |   val number: Int,
        |   private val age: Int,
        |   override val address: String
        |)( ctx: Context,
        |   val ex: Executor)
      """.stripMargin

      val caseTags = TagGenerator.generateTags(testFileCase.parse[Source].get)

      caseTags ~> Seq(
        Tag(None, "SomeClass", false, 1, 11),
        Tag(None, "name", false, 2, 3),
        Tag(None, "number", false, 3, 7),
        Tag(None, "age", true, 4, 15),
        Tag(None, "address", false, 5, 16),
        Tag(None, "ctx", true, 6, 3),
        Tag(None, "ex", false, 7, 7)
      )
    }
  }

  "Should ignore import statements" in {
    val testFileCase =
      s"""
      |import test._
      |case class SomeClass() {
      |
      |   import test2._
      |}
    """.stripMargin

    val caseTags = TagGenerator.generateTags(testFileCase.parse[Source].get)

    caseTags ~> Seq(
      Tag(None, "SomeClass", false, 2, 11)
    )
  }

  "Odd cases" - {
    "infix extraction" in {
      val testFile =
        s"""
        |object Odd {
        | val (d41: Token) +: (d42: Seq[Token]) :+ (d43: Token) = d
        }
        """.stripMargin

      val tags = TagGenerator.generateTags(testFile.parse[Source].get)
      tags ~> Seq(
        Tag(None, "Odd", false, 1, 7),
        Tag(None, "d41", false, 2, 6),
        Tag(None, "d42", false, 2, 22),
        Tag(None, "d43", false, 2, 43),
        Tag(Some("Odd"), "d41", false, 2, 6),
        Tag(Some("Odd"), "d42", false, 2, 22),
        Tag(Some("Odd"), "d43", false, 2, 43)
      )
    }
  }
}
