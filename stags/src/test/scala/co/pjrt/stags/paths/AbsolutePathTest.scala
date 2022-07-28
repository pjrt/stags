package co.pjrt.stags.paths

import java.nio.file.*

import org.scalatest.{Tag as _, *}
import org.scalatest.matchers.*
import org.scalactic.source.Position

class AbsolutePathTest extends freespec.AnyFreeSpec {

  import should.Matchers.*

  private val pwd = System.getProperty("user.dir")

  private def abs(p: String): String =
    pwd + "/" + p

  "relativize" - {

    def testRelative(
        targetS: String,
        filePathS: String,
        expectedS: String
      )(implicit pos: Position
      ) = {

      val target = AbsolutePath.forceAbsolute(Paths.get(targetS))
      val filePath = AbsolutePath.forceAbsolute(Paths.get(filePathS))

      filePath
        .relativeAgainst(target)
        .toString
        .replaceAllLiterally("\\", "/") // windows path sep is backwards
        .shouldBe(expectedS)
    }

    "should modify the filepath to be relative from the target in" - {
      "relative: branch" in {
        testRelative(
          ".git/tag",
          "src/main/stuff/hello.scala",
          "../src/main/stuff/hello.scala"
        )
      }

      "absolute: branch" in {
        testRelative(
          abs(".git/tag"),
          abs("src/main/stuff/hello.scala"),
          "../src/main/stuff/hello.scala"
        )
      }

      "relative: branch: mixed: relative tag" in {
        testRelative(
          ".git/tag",
          abs("src/main/stuff/hello.scala"),
          "../src/main/stuff/hello.scala"
        )
      }

      "absolute: branch: mixed: absolute tag" in {
        testRelative(
          abs(".git/tag"),
          "src/main/stuff/hello.scala",
          "../src/main/stuff/hello.scala"
        )
      }

      "relative: child" in {
        testRelative(
          "src/tags",
          "src/main/stuff/hello.scala",
          "main/stuff/hello.scala"
        )
      }

      "absolute: child" in {
        testRelative(
          abs("src/tags"),
          abs("src/main/stuff/hello.scala"),
          "main/stuff/hello.scala"
        )
      }

      "relative: parent" in {
        testRelative(
          "../tags",
          "src/main/stuff/hello.scala",
          "stags/src/main/stuff/hello.scala"
        )
      }

      "absolute: parent" in {
        testRelative(
          abs("../tags"),
          abs("src/main/stuff/hello.scala"),
          "stags/src/main/stuff/hello.scala"
        )
      }

      "relative: pwd" in {
        testRelative(
          "tags",
          "src/main/stuff/hello.scala",
          "src/main/stuff/hello.scala"
        )
      }

      "absolute: pwd" in {
        testRelative(
          abs("tags"),
          abs("src/main/stuff/hello.scala"),
          "src/main/stuff/hello.scala"
        )
      }

      "relative: pwd: mixed: relative tag" in {
        testRelative(
          "tags",
          abs("src/main/stuff/hello.scala"),
          "src/main/stuff/hello.scala"
        )
      }

      "absolute: pwd: mixed: absolute tag file" in {
        testRelative(
          abs("tags"),
          "src/main/stuff/hello.scala",
          "src/main/stuff/hello.scala"
        )
      }
    }
  }
}
