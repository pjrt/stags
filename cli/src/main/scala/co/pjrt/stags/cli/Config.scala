package co.pjrt.stags.cli

import java.io.File
import java.nio.file.Path

import co.pjrt.stags.GeneratorConfig

sealed trait FileType
object FileType {
  def unapply(str: String): Option[FileType] =
    str match {
      case "scala" | "sc" => Some(Scala)
      case "sources-jar" => Some(SourcesJar)
      case _ => None
    }

  case object Scala extends FileType // A scala source file
  case object SourcesJar extends FileType // a sources.jar file

  implicit val read: scopt.Read[FileType] =
    scopt.Read.reads[FileType] {
      case FileType(a) => a
      case other =>
        throw new IllegalArgumentException(s"Unexpected file type: $other")
    }
}

/**
 * @param files
 *   to generate tags for
 */
final case class Config(
    files: Seq[Path],
    outputFile: Option[Path],
    absoluteTags: Boolean,
    fileTypes: Seq[FileType],
    qualifiedDepth: Int) {

  def appendFile(file: Path): Config =
    this.copy(files = files :+ file)

  def setOutputFile(file: Path): Config =
    this.copy(outputFile = Some(file))

  def setQualifiedDepth(depth: Int): Config =
    this.copy(qualifiedDepth = depth)

  def setAbsoluteTags(bool: Boolean): Config =
    this.copy(absoluteTags = bool)

  def setFileTypes(ts: Seq[FileType]): Config =
    this.copy(fileTypes = ts)

  val canFetchSourcesJar: Boolean = fileTypes.contains(FileType.SourcesJar)
  val canFetchScala: Boolean = fileTypes.contains(FileType.Scala)

  lazy val generatorConfig: GeneratorConfig =
    GeneratorConfig(qualifiedDepth)
}

object Config {

  val default: Config =
    Config(
      Seq.empty,
      None,
      false,
      List(FileType.SourcesJar, FileType.Scala),
      1
    )

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

    opt[Seq[FileType]]("file-types")
      .optional()
      .valueName("<ext>[,<ext>]")
      .action((i, c) => c.setFileTypes(i))
      .text(
        "limit file search to the specified file extensions. Default: scala,sources-jar"
      )

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
    parser.parse(args, Config.default)
}
