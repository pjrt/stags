package co.pjrt.sctags

import scala.meta._

object TagGenerator {

  /**
   * Given a source code's [[Tree]], generate a sequence of [[Tag]]s for it
   */
  def generateTags(tree: Tree): Seq[Tag] = {

    tree.collect {
      case obj: Defn.Object =>
        obj.children.flatMap(c => generateForChildren(obj.name, c))
    }.flatten
  }

  private def generateForChildren(
      lastParent: Term.Name,
      child: Tree
    ): Seq[Tag] =
    child match {
      case d: Defn.Def =>
        Seq(Tag(Some(lastParent), d.name, d.mods, d.pos))
      case obj: Defn.Object =>
        generateTags(obj)
    }
}
