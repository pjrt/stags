package co.pjrt.stags.cli

import java.io.{File, PrintStream, PrintWriter}
import java.nio.file.Paths
import java.util.zip.ZipFile
import scala.util.Try
import scala.collection.JavaConverters._

import scala.meta._

import co.pjrt.stags._
import co.pjrt.stags.paths.AbsolutePath

object Cli {

  final def run_(cwd: AbsolutePath, config: Config): Unit = {
    run(cwd, config)
    ()
  }

  final def run(cwd: AbsolutePath, config: Config): File = {
    val files =
      config.files.flatMap(
        f => fetchScalaFiles(AbsolutePath.fromPath(cwd, f).toFile)
      )
    val outPath = config.outputFile.getOrElse(Paths.get("tags"))
    val out = AbsolutePath.fromPath(cwd, outPath)

    def toTagLine(file: AbsolutePath, scoped: ScopedTag): Seq[TagLine] =
      scoped.mkTagLines(file.relativeAgainst(out), config.qualifiedDepth)
    def toJarTagLine(jar: AbsolutePath, entryPath: String, scoped: ScopedTag): Seq[TagLine] =
      scoped.mkJarTagLine(jar.relativeAgainst(out), entryPath, config.qualifiedDepth)

    val tags: Seq[TagLine] =
      files.flatMap {
        case f if isSourceJar(f) =>
          val zipFile = new ZipFile(f)
          zipFile.entries.asScala.flatMap { entry =>
            if (entry.isDirectory)
              Nil
            else {
              def p = zipFile.getInputStream(entry).parse[Source]
              Try(p).fold(t => Left(t.getMessage), parsedToEither)
                .fold(e => warn(f, e), TagGenerator.generateTags)
                .flatMap(s => toJarTagLine(AbsolutePath.fromPath(cwd, f.toPath), entry.getName, s))
            }
          }
        case f =>
          // DESNOTE(2018-08-03, pjrt): Though parse returns a failure, it can still
          // throw exceptions. See #16
          Try(f.parse[Source]).fold(t => Left(t.getMessage), parsedToEither)
            .fold(e => warn(f, e), TagGenerator.generateTags)
            .flatMap(s => toTagLine(AbsolutePath.fromPath(cwd, f.toPath), s))
      }

    val sortedTags = TagLine.foldCaseSorting(tags)
    val outputFile = out.toFile
    writeFile(outputFile, sortedTags.map(_.vimTagLine))
    outputFile
  }

  private lazy val err: PrintStream = System.err
  private def warn(file: File, msg: String): Seq[ScopedTag] = {

    val warnMsg = s"${LogLevel.warn} Error in ${file.getPath}: $msg"
    err.println(warnMsg)
    Seq.empty
  }

  private def parsedToEither(p: Parsed[Source]): Either[String, Source] =
    p.toEither.fold(e => Left(e.message), Right(_))

  private def isScalaFile(file: File) =
    file.getName.endsWith(".scala")

  private def isSourceJar(file: File) =
    file.getName.endsWith("sources.jar")

  private def fetchScalaFiles(file: File) = {
    if (!file.isDirectory)
      if (isScalaFile(file)) Seq(file)
      else if (isSourceJar(file)) Seq(file)
      else Seq.empty
    else
      fetchFilesFromDir(file)
  }

  private def fetchFilesFromDir(dir: File): Seq[File] =
    dir.listFiles.foldLeft(Seq.empty[File]) {
      case (acc, f) if (f.isDirectory) => acc ++ fetchFilesFromDir(f)
      case (acc, f) if isSourceJar(f)  => acc :+ f
      case (acc, f) if isScalaFile(f)  => acc :+ f
      case (acc, _)                    => acc
    }

  private def writeFile(file: File, lines: Seq[String]): Unit = {

    val pw = new PrintWriter(file)
    val header =
      Seq(
        "!_TAG_FILE_SORTED	2	/0=unsorted, 1=sorted, 2=foldcase/",
        "!_TAG_PROGRAM_AUTHOR	pedro@pjrt.co	//",
        "!_TAG_PROGRAM_NAME	stags",
        "!_TAG_PROGRAM_URL	https://github/pjrt/stags	/official site/",
        s"!_TAG_PROGRAM_VERSION	${build.BuildInfo.version}"
      )

    try {
      (header ++ lines) foreach { t =>
        pw.write(t)
        pw.write("\n")
      }
    } finally {
      pw.close()
    }
  }
}
