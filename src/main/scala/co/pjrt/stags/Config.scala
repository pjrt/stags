package co.pjrt.stags

import java.io.File

import co.pjrt.stags.paths.Path

/**
 * @param files to generate tags for
 */
final case class Config(files: Seq[File], outputFile: Option[Path], qualifiedDepth: Int) {

  def appendFile(file: File): Config =
    this.copy(files = files :+ file)

  def setOutputFile(file: Path): Config =
    this.copy(outputFile = Some(file))

  def setQualifiedDepth(depth: Int): Config =
    this.copy(qualifiedDepth = depth)

  lazy val generatorConfig: GeneratorConfig =
    GeneratorConfig(qualifiedDepth)
}

object Config {

  implicit val zero: scopt.Zero[Config] =
    scopt.Zero.zero(Config(Seq.empty, None, 1))

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
      .action((f, c) => c.setOutputFile(Path.fromNio(f.toPath)))
      .text("location of the tags file. Default: ./tags")

    opt[Int]('d', "qualified-depth")
      .optional()
      .valueName("<int>")
      .validate(i => if (i < 0) failure("option --qualified-depth must be positive") else success)
      .action((i, c) => c.setQualifiedDepth(i))
      .text("depth of qualified tags. Default: 1")
  }

  def parse(args: Array[String]): Option[Config] =
    parser.parse(args, zero.zero)
}

final case class GeneratorConfig(qualifiedDepth: Int)
