package co.pjrt.stags

import scala.meta._

import org.scalatest.{FreeSpec, Matchers}

import Utils._

class TagGeneratorTest extends FreeSpec with Matchers {

  private def abc(q: String*): Scope =
    Scope(Seq("c", "b", "a"), q.reverse.toSeq)
  private def local(q: String*): Scope = Scope(Nil, q.reverse.toList)

  // TODO should test against other limits
  implicit val limit: Int = 10

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
      (abc(), "SomeClass", false),
      (Scope.empty, "hello", false),
      (Scope.empty, "Alias", false),
      (Scope.empty, "Undefined", false)
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
      | @hi
      | type Undefined <: SomeUpper
      | val defined1, defined2 = "hello"
      | val undefined: String
      | var undefined2: String
      |}
      """.stripMargin

    testFile ~> List(
      (abc(), "SomeTrait", false),
      (Scope.empty, "hello", false),
      (Scope.empty, "Alias", false),
      (Scope.empty, "Undefined", false),
      (Scope.empty, "defined1", false),
      (Scope.empty, "defined2", false),
      (Scope.empty, "undefined", false),
      (Scope.empty, "undefined2", false)
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
      (Scope.empty, "SomeObject", false),
      (local("SomeObject"), "whatup", false),
      (local("SomeObject"), "userName", false),
      (local("SomeObject"), "userName2", false),
      (local("SomeObject"), "Alias", false),
      (local("SomeObject"), "Decl", false),
      (local("SomeObject"), "tUserName", false),
      (local("SomeObject"), "tUserName2", false)
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
      (Scope.empty, "SomeObject", false),
      (local("SomeObject"), "InnerObject", false),
      (local("SomeObject", "InnerObject"), "hello", false)
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
      (abc(), "test", false),
      (abc("test"), "whatup", false),
      (abc("test"), "userName", false),
      (abc("test"), "userName2", false),
      (abc("test"), "Alias", false),
      (abc("test"), "Decl", false)
    )
  }

  // DESNOTE(2017-12-06, pjrt): scalameta doesn't seem to understand
  // `package test.some`
  "should work for multiple packages being defined in the same file" ignore {
    val testFile =
      """
      |package a.b.c
      |
      |package test.some {
      | class X
      |}
      |package test2.some2 {
      | class X2
      | package inner.pack {
      |   class InnerX
      | }
      |}
      """.stripMargin

    testFile ~> List(
      (abc("test", "some"), "X", false),
      (abc("test2", "some2"), "X2", false),
      (abc("test2", "some2", "inner", "pack"), "InnerX", false)
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
        (abc(), "SomeObject", false),
        (abc("SomeObject"), "InnerObject", false),
        (abc("SomeObject", "InnerObject"), "privateHello", true),
        (abc("SomeObject", "InnerObject"), "publicHello", false),
        (abc(), "SealedTrait", false),
        (Scope.empty, "f", false),
        (Scope.empty, "protectedHello", false)
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
      |private class PrivateClass {
      |  val x = 1
      |  def y = 1
      |  var z = 1
      |}
      """.stripMargin

    testFile ~>
      List(
        (abc(), "SomeObject", false),
        (abc("SomeObject"), "InnerObject", true),
        (abc("SomeObject", "InnerObject"), "publicHello", true),
        (abc(), "SealedTrait", false),
        (Scope.empty, "protectedHello", true),
        (abc(), "PrivateClass", true),
        (Scope.empty, "x", true),
        (Scope.empty, "y", true),
        (Scope.empty, "z", true)
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
        (Scope.empty, "SomeClass", false),
        (Scope.empty, "name", true),
        (Scope.empty, "number", false),
        (Scope.empty, "age", true)
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
        (Scope.empty, "SomeClass", false),
        (Scope.empty, "name", false),
        (Scope.empty, "number", false),
        (Scope.empty, "age", true),
        (Scope.empty, "address", false),
        (Scope.empty, "ctx", true),
        (Scope.empty, "ex", false)
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
      (Scope.empty, "SomeClass", false)
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
      (Scope.empty, "Odd", false),
      (local("Odd"), "d41", false),
      (local("Odd"), "d42", false),
      (local("Odd"), "d43", false),
      (local("Odd"), "id", false),
      (local("Odd"), "v", false),
      (local("Odd"), "x", false)
    )
  }

  "Should produce a static tag for the value in an implicit class" in {
    val testFile =
      s"""
      |implicit class Class(val x: Int) { }
      """.stripMargin

    testFile ~> Seq(
      (Scope.empty, "Class", false),
      (Scope.empty, "x", true)
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
      (Scope.empty, "SomeThing", false)
    )
  }

  "qualified tags for objects should work with explicit packages" in {
    val testFile =
      """
      |package a.b.c
      |object SomeThing {
      |  def some: Int = {}
      |}
      """.stripMargin

    testFile ~> Seq(
      (abc(), "SomeThing", false),
      (abc("SomeThing"), "some", false)
    )
  }

  "Decl defs should create tags" in {
    val testFile =
      """
      |package a.b.c
      |trait TraitA {
      |  def declDef: Int
      |}
      """.stripMargin

    testFile ~> Seq(
      (abc(), "TraitA", false),
      (Scope.empty, "declDef", false)
    )
  }

  "should ignore @annotations" in {
    val testFile =
      """
      |package a.b.c
      |@typeclass
      |object A {
      |  @someFunkyMacro
      |  @someOtherMacro
      |  type K
      |
      |  @deprecated("dep")
      |  def f: Int = {
      |   1
      |  }
      |  @wow private val x = 2
      |}
      """.stripMargin

    testFile ~>
      Seq(
        (abc(), "A", false),
        (abc("A"), "K", false),
        (abc("A"), "f", false),
        (abc("A"), "x", true)
      )

  }

  "address generation" - {

    "should generate the right address for a class and def" in {

      val testFile =
        """
      |package a.b.c
      |
      |@typeclassish
      |@isshs private class SomeClass(k: SomeClass) {
      | @someMacro
      | def hello(name: String) = name
      |
      | @combo
      | @someTag protected def hi(name: String) = {
      |   name
      | }
      |
      | @anotherMacro
      | val (a, b, c) = someTriple
      |
      | def e: Long = 2
      |}
      """.stripMargin

      val defAddr = "/def \\zshello(name: String) = name/"
      val classAddr = "/private class \\zsSomeClass(k: SomeClass) {/"
      val classAddrK = "/private class SomeClass(\\zsk: SomeClass) {/"
      val defAddr2 = "/protected def \\zshi(name: String) = {/"
      val aAddr = "/val (\\zsa, b, c) = someTriple/"
      val bAddr = "/val (a, \\zsb, c) = someTriple/"
      val cAddr = "/val (a, b, \\zsc) = someTriple/"
      val defE = "/def \\zse: Long = 2/"

      val s = testFile.parse[Source].get
      val actual = TagGenerator.generateTags(s).map(_.tag.tagAddress)
      val expected =
        List(
          defAddr,
          defAddr2,
          classAddr,
          classAddrK,
          aAddr,
          bAddr,
          cAddr,
          defE
        )
      actual should contain theSameElementsAs expected
    }
  }
}
