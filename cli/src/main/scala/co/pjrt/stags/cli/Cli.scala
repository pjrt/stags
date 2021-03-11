package co.pjrt.stags.cli

import java.io.{File, PrintStream, PrintWriter}
import java.nio.file.{Path, Paths}
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
      config.files.flatMap(f =>
        fetchScalaFiles(config, AbsolutePath.fromPath(cwd, f).toFile)
      )
    val outPath = config.outputFile.getOrElse(Paths.get("tags"))
    val out = AbsolutePath.fromPath(cwd, outPath)

    def modifyPath(p: AbsolutePath): Path =
      if (config.absoluteTags) p.path else p.relativeAgainst(out)

    def toTagLine(file: AbsolutePath, scoped: ScopedTag): Seq[TagLine] = {
      scoped.mkTagLines(modifyPath(file), config.qualifiedDepth)
    }
    def toJarTagLine(
        jar: AbsolutePath,
        entryPath: String,
        scoped: ScopedTag
      ): Seq[TagLine] = {
      val jarPath = modifyPath(jar)
      val path = Paths.get(s"zipfile:${jarPath.toString}::$entryPath")
      scoped.mkTagLines(path, config.qualifiedDepth)
    }

    val tags: Seq[TagLine] =
      files.flatMap {
        case f if isSourceJar(f) =>
          val zipFile = new ZipFile(f)
          zipFile.entries.asScala.flatMap { entry =>
            if (entry.getName.endsWith(".scala")) {
              def p = zipFile.getInputStream(entry).parse[Source]
              Try(p)
                .fold(t => Left(t.getMessage), parsedToEither)
                .fold(
                  e => warn(f.getPath + "::" + entry.getName, e),
                  TagGenerator.generateTags
                )
                .flatMap(s =>
                  toJarTagLine(
                    AbsolutePath.fromPath(cwd, f.toPath),
                    entry.getName,
                    s
                  )
                )
            } else {
              Nil
            }
          }
        case f =>
          // DESNOTE(2018-08-03, pjrt): Though parse returns a failure, it can still
          // throw exceptions. See #16
          Try(f.parse[Source])
            .fold(t => Left(t.getMessage), parsedToEither)
            .fold(e => warn(f.getPath, e), TagGenerator.generateTags)
            .flatMap(s => toTagLine(AbsolutePath.fromPath(cwd, f.toPath), s))
      }

    val sortedTags = TagLine.foldCaseSorting(tags)
    val outputFile = out.toFile
    writeFile(outputFile, sortedTags.map(_.vimTagLine))
    outputFile
  }

  private lazy val err: PrintStream = System.err
  private def warn(file: String, msg: String): Seq[ScopedTag] = {

    val warnMsg = s"${LogLevel.warn} Error in $file: $msg"
    err.println(warnMsg)
    Seq.empty
  }

  private def parsedToEither(p: Parsed[Source]): Either[String, Source] =
    p.toEither.fold(e => Left(e.message), Right(_))

  private def isScalaFile(file: File) =
    file.getName.endsWith(".scala")

  private def isSourceJar(file: File) =
    file.getName.endsWith("sources.jar")

  private def fetchScalaFiles(config: Config, file: File) = {
    if (!file.isDirectory) fetchFilesFromDir(config, List(file))
    else fetchFilesFromDir(config, file.listFiles.toList)
  }

  private def fetchFilesFromDir(config: Config, dir: List[File]): Seq[File] =
    dir.foldLeft(Seq.empty[File]) {
      case (acc, f) if (f.isDirectory) =>
        acc ++ fetchFilesFromDir(config, f.listFiles.toList)
      case (acc, f) if isSourceJar(f) && config.canFetchScalaJar => acc :+ f
      case (acc, f) if isScalaFile(f) && config.canFetchScala    => acc :+ f
      case (acc, _)                                              => acc
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
