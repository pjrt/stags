package co.pjrt.stags.cli

import java.io.*
import java.nio.file.*
import java.util.zip.*

import scala.util.Random
import scala.io.Source

import org.scalatest.{Tag as _, *}
import org.scalatest.matchers.*

import co.pjrt.stags.paths.AbsolutePath

final class CliTest extends freespec.AnyFreeSpec with BeforeAndAfter {

  import should.Matchers.*

  private def mkTempDir: Path = {

    Files.createTempDirectory("dir")
  }

  private def runTest(t: AbsolutePath => Assertion): Assertion = {

    val cwd = AbsolutePath.forceAbsolute(mkTempDir)
    t(cwd)
  }

  private def mkFile(cwd: AbsolutePath, ext: String = "scala"): Path = {

    val name = Random.nextInt().abs.toString
    val p = Files.createTempFile(cwd.path, name, ".scala")

    val pw = new PrintWriter(p.toFile)
    val fileContent =
      """|package a.b.c
         |object X""".stripMargin
    pw.write(fileContent)
    pw.close
    p
  }

  private def mkJar(cwd: AbsolutePath): (Path, List[ZipEntry]) = {

    val name = Random.nextInt().abs.toString
    val p = Files.createTempFile(cwd.path, name, "sources.jar")

    val baos = new FileOutputStream(p.toFile)
    val zos = new ZipOutputStream(baos)

    val entry1 = new ZipEntry("file1.scala")
    val entry2 = new ZipEntry("file2.scala")

    val fileContent1 =
      """|package a.b.c
         |object X""".stripMargin
    val fileContent2 =
      """|package a.b.k
         |object Y""".stripMargin

    zos.putNextEntry(entry1)
    zos.write(fileContent1.getBytes())
    zos.closeEntry()
    zos.putNextEntry(entry2)
    zos.write(fileContent2.getBytes())
    zos.closeEntry()
    zos.close()
    (p, List(entry1, entry2))
  }

  private def mkDir(cwd: AbsolutePath): AbsolutePath = {

    val name = Random.nextInt().abs.toString
    AbsolutePath.fromPath(cwd, Files.createTempDirectory(cwd.path, name))
  }

  private def readTags(p: AbsolutePath): List[Path] = {

    val op = for {
      line <- Source.fromFile(p.toFile).getLines()
      if !line.startsWith("!_TAG_")
    } yield {
      line.split('\t').toList match {
        case _ :: file :: _ => Paths.get(file)
        case otherwise => fail(s"Did not get a proper tag. Got $otherwise")
      }
    }
    op.toList
  }

  private val allTypes = List(FileType.SourcesJar, FileType.Scala)

  private def sameElements[A](a: List[A], b: List[A]): Assertion =
    a should contain theSameElementsAs b

  "should capture all scala files in passed" in {
    runTest { cwd =>
      val f1 = mkFile(cwd)
      val up = mkDir(cwd)
      val f2 = mkFile(up)
      val files = List(f1, f2)
      val config = Config(files, None, false, allTypes, 0)
      Cli.run_(cwd, config)

      val tagLoc = AbsolutePath.fromPath(cwd, Paths.get("tags"))
      val tags = readTags(tagLoc)
      val relativizedFiles =
        files.map(AbsolutePath.unsafeAbsolute).map(_.relativeAgainst(tagLoc))
      tags shouldBe relativizedFiles
    }
  }

  "should capture all sc files in passed" in {
    runTest { cwd =>
      val f1 = mkFile(cwd, ext = "sc")
      val up = mkDir(cwd)
      val f2 = mkFile(up, ext = "sc")
      val files = List(f1, f2)
      val config = Config(files, None, false, allTypes, 0)
      Cli.run_(cwd, config)

      val tagLoc = AbsolutePath.fromPath(cwd, Paths.get("tags"))
      val tags = readTags(tagLoc)
      val relativizedFiles =
        files.map(AbsolutePath.unsafeAbsolute).map(_.relativeAgainst(tagLoc))
      tags shouldBe relativizedFiles
    }
  }

  "should capture all source jars files in passed" in {
    runTest { cwd =>
      val (f1, entry1) = mkJar(cwd)
      val up = mkDir(cwd)
      val (f2, entry2) = mkJar(up)
      val files = List(f1, f2)
      val config = Config(files, None, false, allTypes, 0)
      Cli.run_(cwd, config)

      val tagLoc = AbsolutePath.fromPath(cwd, Paths.get("tags"))
      val tags = readTags(tagLoc)
      val relativizedFiles =
        List((f1, entry1), (f2, entry2)).flatMap {
          case (f, es) =>
            val rel = AbsolutePath.unsafeAbsolute(f).relativeAgainst(tagLoc)
            es.map(e => Paths.get(s"zipfile:$rel::${e.getName()}"))
        }
      tags should contain theSameElementsAs (relativizedFiles)
    }
  }

  "should correctly relativize against a tag above" in {
    runTest { cwd =>
      val f1 = mkFile(cwd)
      val files = List(f1)

      val up = mkDir(cwd)
      val tagLoc = AbsolutePath.fromPath(up, Paths.get("tags"))

      val config = Config(files, Some(tagLoc.path), false, allTypes, 0)
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

      val config = Config(files, Some(tagLoc.path), false, allTypes, 0)
      Cli.run_(cwd, config)

      val tags = readTags(tagLoc)
      val relativizedFiles =
        files.map(AbsolutePath.unsafeAbsolute).map(_.relativeAgainst(tagLoc))
      tags shouldBe relativizedFiles
    }
  }

  "should make the paths absolute if the absolute tag is passed" in {
    runTest { cwd =>
      val f1 = mkFile(cwd)
      val up = mkDir(cwd)
      val f2 = mkFile(up)
      val files = List(f1, f2)
      val config = Config(files, None, true, allTypes, 0)
      Cli.run_(cwd, config)

      val tagLoc = AbsolutePath.fromPath(cwd, Paths.get("tags"))
      val tags = readTags(tagLoc)
      tags shouldBe files
    }
  }

  "should only pick up the files that are specified in fileTypes" in {
    allTypes.foreach { t =>
      runTest { cwd =>
        val (f1, entry1) = mkJar(cwd)
        val sf1 = mkFile(cwd)
        val up = mkDir(cwd)
        val (f2, entry2) = mkJar(up)
        val sf2 = mkFile(up)
        val files = List(f1, f2, sf1, sf2)
        val config = Config(files, None, false, List(t), 0)
        Cli.run_(cwd, config)

        val tagLoc = AbsolutePath.fromPath(cwd, Paths.get("tags"))
        val tags = readTags(tagLoc)
        val jarFiles =
          List((f1, entry1), (f2, entry2)).flatMap {
            case (f, es) =>
              val rel = AbsolutePath.unsafeAbsolute(f).relativeAgainst(tagLoc)
              es.map(e => Paths.get(s"zipfile:$rel::${e.getName()}"))
          }
        val scalaFiles =
          List(sf1, sf2)
            .map(AbsolutePath.unsafeAbsolute)
            .map(_.relativeAgainst(tagLoc))

        t match {
          case FileType.SourcesJar =>
            sameElements(tags, jarFiles)
          case FileType.Scala =>
            sameElements(tags, scalaFiles)
        }
      }
    }
  }
}
