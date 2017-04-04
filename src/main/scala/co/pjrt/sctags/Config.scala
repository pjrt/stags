package co.pjrt.stags

import java.io.File
import java.nio.file.Path

/**
 * @param files to generate tags for
 */
case class Config(files: Seq[File], outputFile: Option[Path]) {

  def appendFile(file: File): Config =
    this.copy(files = files :+ file)

  def setOutputFile(file: Path): Config =
    this.copy(outputFile = Some(file))
}

object Config {

  private final val defaultOutput = new File(System.getProperty("user.dir"))

  implicit val zero: scopt.Zero[Config] =
    scopt.Zero.zero(Config(Seq.empty, None))

  final val parser = new scopt.OptionParser[Config]("stags") {
    head("stags", "0.0.1")

    arg[File]("<file>...")
      .required()
      .unbounded()
      .action((f, c) => c.appendFile(f))
      .text("files to generate tags for. Directories are recursively searched")

    opt[File]('o', "output")
      .optional()
      .valueName("<file>")
      .validate(
        f =>
          if (!f.isDirectory) success
          else failure("option --output can't be a directory")
      )
      .action((f, c) => c.setOutputFile(f.toPath))
      .text("location of the tags file. Default: ./tags")
  }

  def parse(args: Array[String]): Option[Config] =
    parser.parse(args, zero.zero)
}
