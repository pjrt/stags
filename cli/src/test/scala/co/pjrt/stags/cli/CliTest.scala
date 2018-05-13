package co.pjrt.stags.cli

import java.io._
import java.nio.file._

import scala.util.Random
import scala.io.Source

import org.scalatest._

import co.pjrt.stags.paths.AbsolutePath

final class CliTest extends FreeSpec with BeforeAndAfter {

  import Matchers._

  private def mkTempDir: Path = {

    Files.createTempDirectory("dir")
  }

  private def runTest(t: AbsolutePath => Assertion): Assertion = {

    val cwd = AbsolutePath.forceAbsolute(mkTempDir)
    t(cwd)
  }

  private def mkFile(cwd: AbsolutePath): Path = {

    val name = Random.nextInt.abs.toString
    val p = Files.createTempFile(cwd.path, name, ".scala")

    val pw = new PrintWriter(p.toFile)
    val fileContent =
      """|package a.b.c
         |object X""".stripMargin
    pw.write(fileContent)
    pw.close
    p
  }

  private def mkDir(cwd: AbsolutePath): AbsolutePath = {

    val name = Random.nextInt.abs.toString
    AbsolutePath.fromPath(cwd, Files.createTempDirectory(cwd.path, name))
  }

  private def readTags(p: AbsolutePath): List[Path] = {

    val op = for {
      line <- Source.fromFile(p.toFile).getLines
      if !line.startsWith("!_TAG_")
    } yield {
      line.split('\t').toList match {
        case _ :: file :: _ => Paths.get(file)
        case otherwise      => fail(s"Did not get a proper tag. Got $otherwise")
      }
    }
    op.toList
  }

  "should capture all scala files in ./" in {
    runTest { cwd =>
      val f1 = mkFile(cwd)
      val up = mkDir(cwd)
      val f2 = mkFile(up)
      val files = List(f1, f2)
      val config = Config(files, None, 0)
      Cli.run_(cwd, config)

      val tagLoc = AbsolutePath.fromPath(cwd, Paths.get("tags"))
      val tags = readTags(tagLoc)
      val relativizedFiles =
        files.map(AbsolutePath.unsafeAbsolute).map(_.relativeAgainst(tagLoc))
      tags shouldBe relativizedFiles
    }
  }

  "should correctly relativize against a tag above" in {
    runTest { cwd =>
      val f1 = mkFile(cwd)
      val files = List(f1)

      val up = mkDir(cwd)
      val tagLoc = AbsolutePath.fromPath(up, Paths.get("tags"))

      val config = Config(files, Some(tagLoc.path), 0)
      Cli.run_(cwd, config)

      val tags = readTags(tagLoc)
      val relativizedFiles =
        files.map(AbsolutePath.unsafeAbsolute).map(_.relativeAgainst(tagLoc))
      tags shouldBe relativizedFiles
    }
  }

  "should correctly relativize against a tag below" in {
    runTest { cwd =>
      val f1 = mkFile(cwd)
      val files = List(f1)

      val down = mkDir(cwd.parent)
      val tagLoc = AbsolutePath.fromPath(down, Paths.get("tags"))

      val config = Config(files, Some(tagLoc.path), 0)
      Cli.run_(cwd, config)

      val tags = readTags(tagLoc)
      val relativizedFiles =
        files.map(AbsolutePath.unsafeAbsolute).map(_.relativeAgainst(tagLoc))
      tags shouldBe relativizedFiles
    }
  }
}
