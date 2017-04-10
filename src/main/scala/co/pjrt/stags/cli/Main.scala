package co.pjrt.stags.cli

import java.io.{File, PrintStream, PrintWriter}

import scala.meta.Parsed

import co.pjrt.stags.{Config, TagGenerator, TagLine}
import co.pjrt.stags.paths.Path

object Main {

  def main(args: Array[String]): Unit =
    Config.parse(args).fold(())(run)

  private lazy val err: PrintStream = System.err

  private final val pwd =
    Path.fromString(System.getProperty("user.dir"))

  private def warn(file: File, msg: String): Unit = {

    val warnMsg = s"${LogLevel.warn} Error in ${file.getPath}: $msg"
    err.println(warnMsg)
  }

  private def run(config: Config): Unit = {
    val files = config.files.flatMap(fetchScalaFiles)
    val outputFile = config.outputFile.getOrElse(Path.fromString("tags"))
    val tags: Seq[TagLine] =
      files.flatMap(
        f =>
          TagGenerator
            .generateTagsForFile(f)(config.generatorConfig)
            .fold((e: Parsed.Error) => {
              warn(f, e.message)
              Seq.empty
            }, identity)
            .map(_.relativize(outputFile))
      )

    val sortedTags = TagLine.foldCaseSorting(tags)
    writeFile(outputFile.toString, sortedTags.map(_.vimTagLine))
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

  private def writeFile(name: String, lines: Seq[String]): Unit = {

    val pw = new PrintWriter(new File(name))
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
