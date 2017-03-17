package co.pjrt.sctags

import scala.meta._
import scala.meta.contrib._

object TagGenerator {

  /**
   * Given some [[Source]] code, generate a sequence of [[Tag]]s for it
   */
  def generateTags(source: Source): Seq[Tag] =
    source.stats.flatMap(tagsForTopLevel(_))

  private def tagsForTopLevel(stat: Stat): Seq[Tag] = {

    stat match {
      case obj: Pkg =>
        // DESNOTE(2017-03-15, prodriguez) Pkg means a `package`. If a package
        // statement is found at the top, then that means EVERYTHING will be a
        // child of it.
        obj.stats.flatMap(tagsForStatement(None, _))
      case obj: Defn.Object =>
        val selfTag = Tag(None, obj.name, obj.mods, obj.name.pos)
        val childrenTags: Seq[Tag] =
          obj.templ.stats
            .map(_.flatMap(tagsForStatement(Some(obj.name), _)))
            .getOrElse(Nil)
        selfTag +: childrenTags
      case obj: Defn.Class =>
        val selfTag = Tag(None, obj.name, obj.mods, obj.name.pos)
        val childrenTags: Seq[Tag] =
          obj.templ.stats
            .map(_.flatMap(tagsForStatement(None, _)))
            .getOrElse(Nil)
        selfTag +: childrenTags
      // DESNOTE(2017-03-17, prodriguez) This repetition is gonna drive me
      // insane. There's GOT to be a better way.
      case obj: Defn.Trait =>
        val selfTag = Tag(None, obj.name, obj.mods, obj.name.pos)
        val childrenTags: Seq[Tag] =
          obj.templ.stats
            .map(_.flatMap(tagsForStatement(None, _)))
            .getOrElse(Nil)
        selfTag +: childrenTags
    }
  }

  private def tagsForStatement(
      lastParent: Option[Term.Name],
      child: Stat
    ): Seq[Tag] = {

    child match {
      // DESNOTE(2017-03-15, prodriguez) There doesn't seem to be a way to
      // access common fields in Defn (mods, name, etc), though looking here
      // https://github.com/scalameta/scalameta/blob/master/scalameta/trees/src/main/scala/scala/meta/Trees.scala#L336
      // it looks like there should be a way.
      case d: Defn.Def =>
        tagsForTerm(lastParent, d.mods, d)
      case d: Defn.Val =>
        d.pats.flatMap {
          case p: Pat.Var.Term => tagsForTerm(lastParent, d.mods, p)
        }
      case obj: Defn =>
        tagsForTopLevel(obj)
    }
  }

  private def tagsForTerm(
      lastParent: Option[Term.Name],
      mods: Seq[Mod],
      term: Member.Term
    ) = {

    val basicTag = Tag(None, term.name, mods, term.name.pos)
    basicTag +: lastParent
      .map(l => Tag(Some(l), term.name, mods, term.name.pos))
      .toSeq
  }
}
