package co.pjrt.stags.cli

// Stolen from Scalafmt
// https://github.com/scalameta/scalafmt/blob/master/core/src/main/scala/org/scalafmt/util/LogLevel.scala
sealed abstract class LogLevel(color: String)(implicit name: sourcecode.Name) {
  override def toString: String = s"[$color${name.value}${Console.RESET}]"
}

object LogLevel {
  case object trace extends LogLevel(Console.RESET)
  case object debug extends LogLevel(Console.GREEN)
  case object info extends LogLevel(Console.BLUE)
  case object warn extends LogLevel(Console.YELLOW)
  case object error extends LogLevel(Console.RED)
}
