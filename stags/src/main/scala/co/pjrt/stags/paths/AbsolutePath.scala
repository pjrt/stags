package co.pjrt.stags.paths

import java.io.File
import java.nio.file.{Path => NioPath}

final class AbsolutePath private (val path: NioPath) {

  def toFile: File = path.toFile

  def relativeAgainst(another: AbsolutePath): NioPath =
    another.path.normalize.getParent.relativize(path).normalize

  def parent: AbsolutePath = new AbsolutePath(path.getParent)

  override def toString: String = path.toString
}

object AbsolutePath {

  def unsafeAbsolute(p: NioPath): AbsolutePath = new AbsolutePath(p)

  def forceAbsolute(p: NioPath): AbsolutePath =
    if (p.isAbsolute) new AbsolutePath(p)
    else new AbsolutePath(p.toAbsolutePath)

  def fromPath(cwd: AbsolutePath, p: NioPath): AbsolutePath =
    if (p.isAbsolute) new AbsolutePath(p)
    else new AbsolutePath(cwd.path.resolve(p))

  def unapply(a: AbsolutePath): Option[NioPath] = Some(a.path)
}
