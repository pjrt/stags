package co.pjrt.stags.cli

object Main {

  def main(args: Array[String]): Unit =
    Config.parse(args).fold(())(Cli.run_)
}
