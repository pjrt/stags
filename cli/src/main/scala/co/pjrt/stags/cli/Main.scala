package co.pjrt.stags.cli

import java.nio.file.Paths

import com.martiansoftware.nailgun.NGContext

import co.pjrt.stags.paths.AbsolutePath

object Main {

  private[this] def absoluteUnsafe(str: String): AbsolutePath =
    AbsolutePath.unsafeAbsolute(Paths.get(str))

  private[this] def userDir: AbsolutePath =
    absoluteUnsafe(System.getProperty("user.dir"))

  def main(args: Array[String]): Unit =
    mainWithCwd(userDir, args)

  def nailMain(ng: NGContext): Unit =
    mainWithCwd(absoluteUnsafe(ng.getWorkingDirectory), ng.getArgs)

  private def mainWithCwd(cwd: AbsolutePath, args: Array[String]): Unit =
    Config.parse(args).fold(())(Cli.run_(cwd, _))

}
