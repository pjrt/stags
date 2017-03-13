package co.pjrt.sctags

import scala.meta._

object TagGenerator {

  /**
   * Given a source code's [[Tree]], generate a sequence of [[Tag]]s for it
   */
  def generateTags(tree: Tree): Seq[Tag] = {

    tree.collect {
      // For objects, generate two tags: one for the basic token and one for
      // Object.TokenName
      case obj: Defn.Object if obj.templ.stats.isDefined =>
        obj.templ.stats.get
          .flatMap(c => generateForChildren(Some(obj.name), c))
      // Classes' functions can't be access in a quantified way, so just create
      // the plain token tag
      case cls: Defn.Class if cls.templ.stats.isDefined =>
        cls.templ.stats.get.flatMap(c => generateForChildren(None, c))
    }.flatten
  }

  private def generateForChildren(
      lastParent: Option[Term.Name],
      child: Tree
    ): Seq[Tag] =
    child match {
      case d: Defn.Def =>
        val basicTag = Tag(None, d.name, d.mods, d.pos)
        basicTag :: lastParent
          .map(l => Tag(Some(l), d.name, d.mods, d.pos))
          .toList
      case obj: Defn.Object =>
        generateTags(obj)
    }
}
