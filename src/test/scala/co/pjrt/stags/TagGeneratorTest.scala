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
      ScopedTag(Seq.empty, "SomeClass", false, 3, 6),
      ScopedTag(Seq.empty, "hello", false, 4, 5),
      ScopedTag(Seq.empty, "Alias", false, 5, 6),
      ScopedTag(Seq.empty, "Undefined", false, 6, 6)
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
      ScopedTag(Seq.empty, "SomeTrait", false, 3, 6),
      ScopedTag(Seq.empty, "hello", false, 4, 5),
      ScopedTag(Seq.empty, "Alias", false, 5, 6),
      ScopedTag(Seq.empty, "Undefined", false, 6, 6),
      ScopedTag(Seq.empty, "defined1", false, 7, 5),
      ScopedTag(Seq.empty, "defined2", false, 7, 15),
      ScopedTag(Seq.empty, "undefined", false, 8, 5)
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
      ScopedTag(Seq.empty, "SomeObject", false, 1, 7),
      ScopedTag(Seq("SomeObject"), "whatup", false, 2, 5),
      ScopedTag(Seq("SomeObject"), "userName", false, 3, 5),
      ScopedTag(Seq("SomeObject"), "userName2", false, 3, 15),
      ScopedTag(Seq("SomeObject"), "Alias", false, 4, 6),
      ScopedTag(Seq("SomeObject"), "Decl", false, 5, 6),
      ScopedTag(Seq("SomeObject"), "tUserName", false, 6, 6),
      ScopedTag(Seq("SomeObject"), "tUserName2", false, 6, 17)
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
      ScopedTag(Seq.empty, "SomeObject", false, 1, 7),
      ScopedTag(Seq("SomeObject"), "InnerObject", false, 2, 8),
      ScopedTag(Seq("InnerObject"), "hello", false, 3, 7)
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
      ScopedTag(Seq.empty, "test", false, 3, 15),
      ScopedTag(Seq("test"), "whatup", false, 4, 5),
      ScopedTag(Seq("test"), "userName", false, 5, 5),
      ScopedTag(Seq("test"), "userName2", false, 5, 15),
      ScopedTag(Seq("test"), "Alias", false, 6, 6),
      ScopedTag(Seq("test"), "Decl", false, 7, 6)
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
        ScopedTag(Seq.empty, "SomeObject", false, 3, 7),
        ScopedTag(Seq("SomeObject"), "InnerObject", false, 4, 22),
        ScopedTag(Seq("InnerObject", "SomeObject"), "privateHello", true, 5, 15),
        ScopedTag(Seq("InnerObject", "SomeObject"), "publicHello", false, 6, 7),
        ScopedTag(Seq.empty, "SealedTrait", false, 9, 13),
        ScopedTag(Seq.empty, "f", false, 10, 12),
        ScopedTag(Seq.empty, "protectedHello", false, 11, 22)
      )
  }

  "Should generate static tags for private[enclosing] and private[this]" in {
    val testFile =
      """
      |package co.pjrt.ctags.test
      |
      |object SomeObject {
      | private[SomeObject] object InnerObject {
      |   def publicHello(name: String) = name
      | }
      |}
      |sealed trait SealedTrait {
      |  private[this] def protectedHello(name: String) = name
      |}
      """.stripMargin

    val tags = TagGenerator.generateTags(testFile.parse[Source].get)

    tags ~>
      List(
        ScopedTag(Seq.empty, "SomeObject", false, 3, 7),
        ScopedTag(Seq("SomeObject"), "InnerObject", true, 4, 28),
        ScopedTag(Seq("InnerObject", "SomeObject"), "publicHello", false, 5, 7),
        ScopedTag(Seq.empty, "SealedTrait", false, 8, 13),
        ScopedTag(Seq.empty, "protectedHello", true, 9, 20)
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
        ScopedTag(Seq.empty, "SomeClass", false, 1, 6),
        ScopedTag(Seq.empty, "name", true, 2, 3),
        ScopedTag(Seq.empty, "number", false, 3, 7),
        ScopedTag(Seq.empty, "age", true, 4, 15)
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
        ScopedTag(Seq.empty, "SomeClass", false, 1, 11),
        ScopedTag(Seq.empty, "name", false, 2, 3),
        ScopedTag(Seq.empty, "number", false, 3, 7),
        ScopedTag(Seq.empty, "age", true, 4, 15),
        ScopedTag(Seq.empty, "address", false, 5, 16),
        ScopedTag(Seq.empty, "ctx", true, 6, 3),
        ScopedTag(Seq.empty, "ex", false, 7, 7)
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
      ScopedTag(Seq.empty, "SomeClass", false, 2, 11)
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
        ScopedTag(Seq.empty, "Odd", false, 1, 7),
        ScopedTag(Seq("Odd"), "d41", false, 2, 6),
        ScopedTag(Seq("Odd"), "d42", false, 2, 22),
        ScopedTag(Seq("Odd"), "d43", false, 2, 43)
      )
    }

    "implicit class value should be static" in {
      val testFile =
        s"""
        |implicit class Class(val x: Int) { }
        """.stripMargin

      val tags = TagGenerator.generateTags(testFile.parse[Source].get)
      tags ~> Seq(
        ScopedTag(Seq.empty, "Class", false, 1, 15),
        ScopedTag(Seq.empty, "x", true, 1, 25)
      )
    }
  }
}
