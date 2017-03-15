package co.pjrt.sctags

import scala.meta._
import scala.meta.contrib._

object TagGenerator {

  /**
   * Given some [[Source]] code, generate a sequence of [[Tag]]s for it
   */
  def generateTags(source: Source): Seq[Tag] =
    generate(source)

  private def generate(tree: Tree): Seq[Tag] = {

    tree.descendants.map {
      // For objects, generate two tags: one for the basic token and one for
      // Object.TokenName
      case obj: Defn.Object if obj.templ.stats.isDefined =>
        obj.templ.stats.get
          .flatMap(c => generateForChildren(Some(obj.name), c))
      // Classes' functions can't be access in a quantified way, so just create
      // the plain token tag
      case cls: Defn.Class if cls.templ.stats.isDefined =>
        cls.templ.stats.get.flatMap(c => generateForChildren(None, c))
      case _ => Seq.empty
    }.flatten
  }

  private def generateForChildren(
      lastParent: Option[Term.Name],
      child: Tree
    ): Seq[Tag] = {

    child match {
      case d: Defn.Def =>
        val basicTag = Tag(None, d.name, d.mods, d.name.pos)
        basicTag :: lastParent
          .map(l => Tag(Some(l), d.name, d.mods, d.name.pos))
          .toList
      case obj: Defn.Object =>
        generate(obj)
    }
  }
}
