package co.pjrt.sctags

import java.io.File

import scala.meta._

object TagGenerator {

  /**
   * Given some [[Source]] code, generate a sequence of [[Tag]]s for it
   */
  def generateTags(source: Source): Seq[Tag] =
    source.stats.flatMap(tagsForTopLevel(_))

  def generateTagsForFile(file: File): Either[Parsed.Error, Seq[Tag]] =
    file.parse[Source] match {
      case Parsed.Success(s) => Right(generateTags(s))
      case err: Parsed.Error => Left(err)

    }

  def generateTagsForFileName(
      fileName: String
    ): Either[Parsed.Error, Seq[Tag]] =
    generateTagsForFile(new File(fileName))

  private def tagsForTopLevel(stat: Stat): Seq[Tag] = {

    stat match {
      case obj: Pkg =>
        // DESNOTE(2017-03-15, pjrt) Pkg means a `package`. If a package
        // statement is found at the top, then that means EVERYTHING will be a
        // child of it.
        obj.stats.flatMap(tagsForTopLevel)
      case st =>
        tagsForStatement(None, st)
    }
  }

  private def tagsForStatement(
      lastParent: Option[Term.Name],
      child: Stat
    ): Seq[Tag] = {

    child match {
      // DESNOTE(2017-03-15, pjrt) There doesn't seem to be a way to
      // access common fields in Defn (mods, name, etc), though looking here
      // https://github.com/scalameta/scalameta/blob/master/scalameta/trees/src/main/scala/scala/meta/Trees.scala#L336
      // it looks like there should be a way.
      case d: Defn.Def => tagsForMember(lastParent, d.mods, d)
      case d: Defn.Val =>
        d.pats.flatMap {
          // TODO:pjrt what about others?
          case p: Pat.Var.Term => tagsForMember(lastParent, d.mods, p)
        }
      case d: Decl.Val =>
        d.pats.flatMap {
          // TODO:pjrt what about others?
          case p: Pat.Var.Term => tagsForMember(lastParent, d.mods, p)
        }
      case d: Defn.Type => tagsForMember(lastParent, d.mods, d)
      case d: Decl.Type => tagsForMember(lastParent, d.mods, d)

      case d: Defn.Object =>
        tagsForMember(lastParent, d.mods, d) ++
          d.templ.stats
            .map(_.flatMap(tagsForStatement(Some(d.name), _)))
            .getOrElse(Nil)
      case d: Pkg.Object =>
        tagsForMember(lastParent, d.mods, d) ++
          d.templ.stats
            .map(_.flatMap(tagsForStatement(Some(d.name), _)))
            .getOrElse(Nil)

      case d: Defn.Trait =>
        tagsForMember(lastParent, d.mods, d) ++
          d.templ.stats
            .map(_.flatMap(tagsForStatement(None, _)))
            .getOrElse(Nil)
      case d: Defn.Class =>
        tagsForMember(lastParent, d.mods, d) ++
          tagsForCtorParams(d.isCaseClass, d.ctor.paramss) ++
          d.templ.stats
            .map(_.flatMap(tagsForStatement(None, _)))
            .getOrElse(Nil)

      case _ => Seq.empty
    }
  }

  private def tagsForMember(
      lastParent: Option[Term.Name],
      mods: Seq[Mod],
      term: Member
    ) = {

    val static = isStatic(lastParent.map(_.value), mods)
    val basicTag = Tag(None, term.name, static, term.name.pos)
    basicTag +: lastParent
      .map(l => Tag(Some(l), term.name, static, term.name.pos))
      .toSeq
  }

  // When generating tags for Ctors of classes we need to see if it is a case
  // class. If it is, then params IN THE FIRST PARAM GROUP with no mods are
  // NOT static. Other params are still static.
  private def tagsForCtorParams(
      isCase: Boolean,
      paramss: Seq[Seq[Term.Param]]
    ) = {
    paramss match {
      case first +: rem =>
        val firstIsStatic: Term.Param => Boolean = p =>
          if (isCase) isStatic(None, p.mods) else isStaticCtorParam(p)
        first.map(p => Tag(None, p.name, firstIsStatic(p), p.name.pos)) ++
          (for {
            pGroup <- rem
            param <- pGroup
          } yield {
            Tag(
              None,
              param.name.value,
              isStaticCtorParam(param),
              param.name.pos
            )
          })
      case Seq() => Nil
    }
  }

  private def isStaticCtorParam(param: Term.Param) =
    param.mods.isEmpty || isStatic(None, param.mods)

  private def isStatic(prefix: Option[String], mods: Seq[Mod]): Boolean =
    mods
      .collect {
        case Mod.Private(Name.Anonymous()) => true
        case Mod.Private(Name.Indeterminate(name)) =>
          // DESNOTE(2017-03-21, prodriguez): If the name of the private thing
          // is the parent object, then it is just private.
          if (prefix.contains(name))
            true
          else
            false
        case Mod.ValParam() => false
        case Mod.VarParam() => false
      }
      .headOption
      .getOrElse(false)
}
