package co.pjrt.sctags

import java.io.File

/**
 * @param files to generate tags for
 */
case class Config(files: Seq[File]) {

  def appendFile(file: File): Config =
    this.copy(files = files :+ file)
}

object Config {

  implicit val zero: scopt.Zero[Config] =
    scopt.Zero.zero(Config(Seq.empty))

  final val parser = new scopt.OptionParser[Config]("stags") {
    head("stags", "0.0.1")

    arg[File]("<file>...")
      .required()
      .unbounded()
      .action((f, c) => c.appendFile(f))
      .text("files to generate tags for. Directories are recursively searched")
  }

  def parse(args: Array[String]): Option[Config] =
    parser.parse(args, zero.zero)
}
