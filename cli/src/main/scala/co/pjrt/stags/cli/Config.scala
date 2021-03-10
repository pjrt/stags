package co.pjrt.stags.cli

import java.io.File
import java.nio.file.Path

import co.pjrt.stags.GeneratorConfig

/**
 * @param files to generate tags for
 */
final case class Config(
    files: Seq[Path],
    outputFile: Option[Path],
    absoluteTags: Boolean,
    qualifiedDepth: Int) {

  def appendFile(file: Path): Config =
    this.copy(files = files :+ file)

  def setOutputFile(file: Path): Config =
    this.copy(outputFile = Some(file))

  def setQualifiedDepth(depth: Int): Config =
    this.copy(qualifiedDepth = depth)

  def setAbsoluteTags(bool: Boolean): Config =
    this.copy(absoluteTags = bool)

  lazy val generatorConfig: GeneratorConfig =
    GeneratorConfig(qualifiedDepth)
}

object Config {

  implicit val zero: scopt.Zero[Config] =
    scopt.Zero.zero(Config(Seq.empty, None, false, 1))

  final val parser = new scopt.OptionParser[Config]("stags") {
    head("stags", build.BuildInfo.version)

    help("help")
    version("version")

    arg[File]("<file>...")
      .required()
      .unbounded()
      .action((f, c) => c.appendFile(f.toPath))
      .text("files to generate tags for. Directories are recursively searched")

    opt[File]('o', "output")
      .optional()
      .valueName("<file>")
      .validate(f =>
        if (!f.isDirectory) success
        else failure("option --output can't be a directory")
      )
      .action((f, c) => c.setOutputFile(f.toPath))
      .text("location of the tags file. Default: ./tags")

    opt[Int]('d', "qualified-depth")
      .optional()
      .valueName("<int>")
      .validate(i =>
        if (i < 0) failure("option --qualified-depth must be positive")
        else success
      )
      .action((i, c) => c.setQualifiedDepth(i))
      .text("depth of qualified tags. Default: 1")

    opt[Unit]("absolute-files")
      .optional()
      .action((_, c) => c.setAbsoluteTags(true))
      .text(
        "set files to be absolute (/path/to/file instead of ./file) in the tags files"
      )
    note(
      """
      |By default tags are written relative to the 'output' file, but if you are tagging jars
      |you may want to enable --absolute-files and disable 'tagrelative' in vim,
      |as zipfiles won't then be understood correctly by vim""".stripMargin
    )
  }

  def parse(args: Array[String]): Option[Config] =
    parser.parse(args, zero.zero)
}
