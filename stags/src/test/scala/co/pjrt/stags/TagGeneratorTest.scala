package co.pjrt.stags

import org.scalatest.{FreeSpec, Matchers}

import Utils._

class TagGeneratorTest extends FreeSpec with Matchers {

  def abc(q: String*): Scope = Scope(Seq("c", "b", "a"), q.toSeq)

  // TODO should test against other limits
  implicit val limit: Int = 1

  "Should generate unqualified tags for classes" in {
    val testFile =
      """
      |package a.b.c
      |
      |class SomeClass() {
      | def hello(name: String) = name
      | type Alias = String
      | type Undefined
      |}
      """.stripMargin

    testFile ~> List(
      ScopedTag(abc(), "SomeClass", false, 3, 6),
      ScopedTag(Scope.empty, "hello", false, 4, 5),
      ScopedTag(Scope.empty, "Alias", false, 5, 6),
      ScopedTag(Scope.empty, "Undefined", false, 6, 6)
    )
  }

  "Should generate unqualified tags for traits" in {
    val testFile =
      """
      |package a.b.c
      |
      |trait SomeTrait {
      | def hello(name: String) = name
      | type Alias = String
      | type Undefined <: SomeUpper
      | val defined1, defined2 = "hello"
      | val undefined: String
      |}
      """.stripMargin

    testFile ~> List(
      ScopedTag(abc(), "SomeTrait", false, 3, 6),
      ScopedTag(Scope.empty, "hello", false, 4, 5),
      ScopedTag(Scope.empty, "Alias", false, 5, 6),
      ScopedTag(Scope.empty, "Undefined", false, 6, 6),
      ScopedTag(Scope.empty, "defined1", false, 7, 5),
      ScopedTag(Scope.empty, "defined2", false, 7, 15),
      ScopedTag(Scope.empty, "undefined", false, 8, 5)
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

    testFile ~> List(
      ScopedTag(Scope.empty, "SomeObject", false, 1, 7),
      ScopedTag(Scope(Seq("SomeObject")), "whatup", false, 2, 5),
      ScopedTag(Scope(Seq("SomeObject")), "userName", false, 3, 5),
      ScopedTag(Scope(Seq("SomeObject")), "userName2", false, 3, 15),
      ScopedTag(Scope(Seq("SomeObject")), "Alias", false, 4, 6),
      ScopedTag(Scope(Seq("SomeObject")), "Decl", false, 5, 6),
      ScopedTag(Scope(Seq("SomeObject")), "tUserName", false, 6, 6),
      ScopedTag(Scope(Seq("SomeObject")), "tUserName2", false, 6, 17)
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

    testFile ~> List(
      ScopedTag(Scope.empty, "SomeObject", false, 1, 7),
      ScopedTag(Scope(Seq("SomeObject")), "InnerObject", false, 2, 8),
      ScopedTag(Scope(Seq("InnerObject")), "hello", false, 3, 7)
    )
  }

  "Should generate qualified AND unqualified tags for package object members" in {
    val testFile =
      """
      |package a.b.c
      |
      |package object test {
      | def whatup(name: String) = name
      | val userName, userName2 = "hello"
      | type Alias = Int
      | type Decl <: SomeUpper
      |}
      """.stripMargin

    testFile ~> List(
      ScopedTag(abc(), "test", false, 3, 15),
      ScopedTag(abc("test"), "whatup", false, 4, 5),
      ScopedTag(abc("test"), "userName", false, 5, 5),
      ScopedTag(abc("test"), "userName2", false, 5, 15),
      ScopedTag(abc("test"), "Alias", false, 6, 6),
      ScopedTag(abc("test"), "Decl", false, 7, 6)
    )
  }

  "Should capture modifiers and generate the correct isStatic" in {
    val testFile =
      """
      |package a.b.c
      |
      |object SomeObject {
      | private[c] object InnerObject {
      |   private def privateHello(name: String) = name
      |   def publicHello(name: String) = name
      | }
      |}
      |sealed trait SealedTrait {
      |  final def f(name: String) = name
      |  protected[test] def protectedHello(name: String) = name
      |}
      """.stripMargin

    testFile ~>
      List(
        ScopedTag(abc(), "SomeObject", false, 3, 7),
        ScopedTag(abc("SomeObject"), "InnerObject", false, 4, 19),
        ScopedTag(
          abc("InnerObject", "SomeObject"),
          "privateHello",
          true,
          5,
          15
        ),
        ScopedTag(
          abc("InnerObject", "SomeObject"),
          "publicHello",
          false,
          6,
          7
        ),
        ScopedTag(abc(), "SealedTrait", false, 9, 13),
        ScopedTag(Scope.empty, "f", false, 10, 12),
        ScopedTag(Scope.empty, "protectedHello", false, 11, 22)
      )
  }

  "Should generate static tags for private[enclosing] and private[this]" in {
    val testFile =
      """
      |package a.b.c
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

    testFile ~>
      List(
        ScopedTag(abc(), "SomeObject", false, 3, 7),
        ScopedTag(abc("SomeObject"), "InnerObject", true, 4, 28),
        ScopedTag(
          abc("InnerObject", "SomeObject"),
          "publicHello",
          false,
          5,
          7
        ),
        ScopedTag(abc(), "SealedTrait", false, 8, 13),
        ScopedTag(Scope.empty, "protectedHello", true, 9, 20)
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

      testFileClass ~> Seq(
        ScopedTag(Scope.empty, "SomeClass", false, 1, 6),
        ScopedTag(Scope.empty, "name", true, 2, 3),
        ScopedTag(Scope.empty, "number", false, 3, 7),
        ScopedTag(Scope.empty, "age", true, 4, 15)
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

      testFileCase ~> Seq(
        ScopedTag(Scope.empty, "SomeClass", false, 1, 11),
        ScopedTag(Scope.empty, "name", false, 2, 3),
        ScopedTag(Scope.empty, "number", false, 3, 7),
        ScopedTag(Scope.empty, "age", true, 4, 15),
        ScopedTag(Scope.empty, "address", false, 5, 16),
        ScopedTag(Scope.empty, "ctx", true, 6, 3),
        ScopedTag(Scope.empty, "ex", false, 7, 7)
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

    testFileCase ~> Seq(
      ScopedTag(Scope.empty, "SomeClass", false, 2, 11)
    )
  }

  "Should extract pattern extractions" in {
    val testFile =
      s"""
      |object Odd {
      | val (d41: Token) +: (d42: Seq[Token]) :+ (d43: Token) = d
      | val Some((id: Int, v: String)) = k
      | val List(1, 2, x @ _*) = List(1, 2, 3, 4)
      | val Seq(1, 2, _*) = Seq(1, 2, 3, 4)
      }
      """.stripMargin

    testFile ~> Seq(
      ScopedTag(Scope.empty, "Odd", false, 1, 7),
      ScopedTag(Scope(Seq("Odd")), "d41", false, 2, 6),
      ScopedTag(Scope(Seq("Odd")), "d42", false, 2, 22),
      ScopedTag(Scope(Seq("Odd")), "d43", false, 2, 43),
      ScopedTag(Scope(Seq("Odd")), "id", false, 3, 11),
      ScopedTag(Scope(Seq("Odd")), "v", false, 3, 20),
      ScopedTag(Scope(Seq("Odd")), "x", false, 4, 16)
    )
  }

  "Should produce a static tag for the value in an implicit class" in {
    val testFile =
      s"""
      |implicit class Class(val x: Int) { }
      """.stripMargin

    testFile ~> Seq(
      ScopedTag(Scope.empty, "Class", false, 1, 15),
      ScopedTag(Scope.empty, "x", true, 1, 25)
    )
  }

  "Should NOT produce a tag for underscore (_) vals" in {
    val testFile =
      s"""
      |class SomeThing {
      | val _ = 123
      |}
      """.stripMargin

    testFile ~> Seq(
      ScopedTag(Scope.empty, "SomeThing", false, 1, 6)
    )
  }

  "qualified tags for objects should work with explicit packages" in {
    val testFile =
      """
      |package a.b.c.d
      |object SomeThing {
      |  def some: Int = {}
      |}
      """.stripMargin

      testFile ~> Seq(
        ScopedTag(Scope.empty, "SomeThing", false, 2, 7),
        ScopedTag(Scope(Seq("SomeThing")), "some", false, 3, 6)
      )
  }
}
