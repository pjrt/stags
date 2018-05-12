package co.pjrt.stags.cli

import com.martiansoftware.nailgun.NGContext

object Main {

  def main(args: Array[String]): Unit =
    Config.parse(args).fold(())(Cli.run_)

  def nailMain(ng: NGContext): Unit =
    main(ng.getArgs)
}
