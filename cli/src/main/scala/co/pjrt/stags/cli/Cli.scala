package co.pjrt.stags.cli

import java.io.{File, PrintStream, PrintWriter}

import scala.meta.Parsed

import co.pjrt.stags.{TagGenerator, TagLine}
import co.pjrt.stags.paths.Path

object Cli {

  final def run_(config: Config): Unit = {
    run(config)
    ()
  }

  final def run(config: Config): File = {
    val files = config.files.flatMap(fetchScalaFiles)
    val outPath = config.outputFile.getOrElse(Path.fromString("tags"))
    val tags: Seq[TagLine] =
      files.flatMap(
        f =>
          TagGenerator
            .generateTagsForFile(f)(config.generatorConfig)
            .fold((e: Parsed.Error) => {
              warn(f, e.message)
              Seq.empty
            }, identity)
            .map(_.relativize(outPath))
      )

    val sortedTags = TagLine.foldCaseSorting(tags)
    val outputFile = outPath.nioPath.toFile
    writeFile(outputFile, sortedTags.map(_.vimTagLine))
    outputFile
  }

  private lazy val err: PrintStream = System.err
  private def warn(file: File, msg: String): Unit = {

    val warnMsg = s"${LogLevel.warn} Error in ${file.getPath}: $msg"
    err.println(warnMsg)
  }

  private def isScalaFile(file: File) =
    file.getName.endsWith(".scala")

  private def fetchScalaFiles(file: File) = {
    if (!file.isDirectory)
      if (isScalaFile(file)) Seq(file)
      else Seq.empty
    else
      fetchFilesFromDir(file)
  }

  private def fetchFilesFromDir(dir: File): Seq[File] =
    dir.listFiles.foldLeft(Seq.empty[File]) {
      case (acc, f) if (f.isDirectory) => acc ++ fetchFilesFromDir(f)
      case (acc, f) if isScalaFile(f) => acc :+ f
      case (acc, _) => acc
    }

  private def writeFile(file: File, lines: Seq[String]): Unit = {

    val pw = new PrintWriter(file)
    val header =
      Seq(
        "!_TAG_FILE_SORTED	2	/0=unsorted, 1=sorted, 2=foldcase/",
        "!_TAG_PROGRAM_AUTHOR	pedro@pjrt.co	//",
        "!_TAG_PROGRAM_NAME	stags",
        "!_TAG_PROGRAM_URL	https://github/pjrt/stags	/official site/",
        "!_TAG_PROGRAM_VERSION	0.0.0"
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
