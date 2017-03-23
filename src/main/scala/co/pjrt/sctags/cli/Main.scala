package co.pjrt.sctags.cli

import java.io.{File, PrintWriter}

import scala.meta.Parsed
import scala.util.Sorting

import co.pjrt.sctags.{Config, TagGenerator}

object Main {

  private def userDir =
    new File(System.getProperty("user.dir"))

  def main(args: Array[String]): Unit =
    Config.parse(args).fold(())(run)

  private def run(config: Config): Unit = {
    val files = config.files.flatMap { file =>
      if (!file.isDirectory)
        Seq(file)
      else
        fetchFilesFromDir(file)
    }
    val tags: Seq[String] =
      files.flatMap(
        f =>
          TagGenerator
            .generateTagsForFile(f)
            .fold((e: Parsed.Error) => throw e.details, identity)
            .map(_.vimTagLine(f.getPath))
      )

    val sortedTags = Sorting.stableSort(tags)
    writeFile("tags", sortedTags)
  }

  private def fetchFilesFromDir(dir: File): Seq[File] =
    dir.listFiles.foldLeft(Seq.empty[File]) {
      case (acc, f) =>
        if (f.isDirectory)
          acc ++ fetchFilesFromDir(f)
        else
          acc :+ f
    }

  private def writeFile(name: String, lines: Seq[String]): Unit = {

    val pw = new PrintWriter(new File(name))
    val header =
      Seq(
        "!_TAG_FILE_SORTED	1	/0=unsorted, 1=sorted, 2=foldcase/",
        "!_TAG_PROGRAM_AUTHOR	pedro@pjrt.co	//",
        "!_TAG_PROGRAM_NAME	stags",
        "!_TAG_PROGRAM_URL	https://github/pjrt/stags	/official site/",
        "!_TAG_PROGRAM_VERSION	0.0.0"
      )

    try {
      (header ++ lines) foreach { t =>
        pw.write("\n")
        pw.write(t)
      }
    } finally {
      pw.close()
    }
  }
}
