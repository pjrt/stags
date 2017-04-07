package co.pjrt.stags.paths

import java.nio.file.{Path => NioPath, Paths}

/**
 * A wrapper around [[java.nio.file.Path]] that ensures the path is absolute
 * and normalized
 */
class Path private (val nioPath: NioPath) {

  lazy val getParent: Path = new Path(nioPath.getParent)

  def relativize(other: Path): Path =
    new Path(nioPath.relativize(other.nioPath))

  def toAbsolute: Path = new Path(nioPath.toAbsolutePath)

  override def toString: String = nioPath.toString
}

object Path {

  def fromNio(nio: NioPath): Path = new Path(nio.toAbsolutePath.normalize)

  def fromString(str: String): Path = fromNio(Paths.get(str))
}
