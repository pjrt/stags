package co.pjrt.stags

import java.io.File

import scala.meta._

import co.pjrt.stags.paths.Path

object TagGenerator {

  /**
   * Given some [[Source]] code, generate a sequence of [[Tag]]s for it
   */
  def generateTags(source: Source): Seq[ScopedTag] =
    source.stats.flatMap(tagsForTopLevel(_))

  def generateTagsForFile(
      file: File
    )(implicit conf: GeneratorConfig
    ): Either[Parsed.Error, Seq[TagLine]] =
    file.parse[Source] match {
      case Parsed.Success(s) =>
        Right(
          generateTags(s).flatMap(
            _.mkTagLines(Path.fromNio(file.toPath), conf.qualifiedDepth)
          )
        )
      case err: Parsed.Error =>
        Left(err)

    }

  def generateTagsForFileName(
      fileName: String
    )(implicit conf: GeneratorConfig
    ): Either[Parsed.Error, Seq[TagLine]] =
    generateTagsForFile(new File(fileName))

  private def tagsForTopLevel(stat: Stat): Seq[ScopedTag] = {

    stat match {
      case obj: Pkg =>
        // DESNOTE(2017-03-15, pjrt) Pkg means a `package`. If a package
        // statement is found at the top, then that means EVERYTHING will be a
        // child of it.
        obj.stats.flatMap(tagsForTopLevel)
      case st =>
        tagsForStatement(Seq.empty, st)
    }
  }

  private def tagsForStatement(
      scope: Seq[Term.Name],
      child: Stat
    ): Seq[ScopedTag] = {

    child match {
      // DESNOTE(2017-03-15, pjrt) There doesn't seem to be a way to
      // access common fields in Defn (mods, name, etc), though looking here
      // https://github.com/scalameta/scalameta/blob/master/scalameta/trees/src/main/scala/scala/meta/Trees.scala#L336
      // it looks like there should be a way.
      case d: Defn.Def => Seq(tagsForMember(scope, d.mods, d))
      case d: Defn.Val => d.pats.flatMap(getFromPats(scope, d.mods, _))
      case d: Decl.Val => d.pats.flatMap(getFromPats(scope, d.mods, _))
      case d: Defn.Type => Seq(tagsForMember(scope, d.mods, d))
      case d: Decl.Type => Seq(tagsForMember(scope, d.mods, d))

      case d: Defn.Object =>
        tagsForMember(scope, d.mods, d) +:
          d.templ.stats
          .map(_.flatMap(tagsForStatement(d.name +: scope, _)))
          .getOrElse(Nil)
      case d: Pkg.Object =>
        tagsForMember(scope, d.mods, d) +:
          d.templ.stats
          .map(_.flatMap(tagsForStatement(d.name +: scope, _)))
          .getOrElse(Nil)

      case d: Defn.Trait =>
        tagsForMember(scope, d.mods, d) +:
          d.templ.stats
          .map(_.flatMap(tagsForStatement(Seq.empty, _)))
          .getOrElse(Nil)
      case d: Defn.Class =>
        val ctorParamTags: Seq[ScopedTag] =
          if (d.isImplicitClass)
            // DESNOTE(2017-04-05, pjrt) This can only possibly have a single
            // element (due to how implicit classes work). However, parsing
            // of other cases is valid, though they would fail to compile. Just
            // to be in the safe side, flatten and map instead of _.head.head
            d.ctor.paramss.flatten.map(tagForImplicitClassParam)
          else
            tagsForCtorParams(d.isCaseClass, d.ctor.paramss)

        (tagsForMember(scope, d.mods, d) +: ctorParamTags) ++
          d.templ.stats
            .map(_.flatMap(tagsForStatement(Seq.empty, _)))
            .getOrElse(Nil)

      case _ => Seq.empty
    }
  }

  private def getFromPats(
      scope: Seq[Term.Name],
      mods: Seq[Mod],
      pat: Pat.Arg
    ): Seq[ScopedTag] = {

    def getFromPat(p: Pat.Arg) = getFromPats(scope, mods, p)
    pat match {
      case p: Pat.Var.Term => Seq(tagsForMember(scope, mods, p))
      case Pat.Typed(p, _) => getFromPat(p)
      case Pat.Tuple(args) => args.flatMap(getFromPat)
      case Pat.ExtractInfix(lhs, _, pats) =>
        getFromPat(lhs) ++ pats.flatMap(getFromPat)
    }
  }

  private def tagsForMember(
      scope: Seq[Term.Name],
      mods: Seq[Mod],
      term: Member
    ) = {

    val static = isStatic(scope.map(_.value), mods)
    ScopedTag(scope, term.name, static, term.name.pos)
  }

  // When we are dealing with implicit classes, the parameter oughts to be
  // static. Even though it can be accessed from the outside, it is very
  // unlikely to ever be.
  private def tagForImplicitClassParam(param: Term.Param) =
    ScopedTag(Seq.empty, param.name, true, param.name.pos)

  // When generating tags for Ctors of classes we need to see if it is a case
  // class. If it is, then params IN THE FIRST PARAM GROUP with no mods are
  // NOT static. Other params are still static.
  private def tagsForCtorParams(
      isCase: Boolean,
      paramss: Seq[Seq[Term.Param]]
    ): Seq[ScopedTag] = {
    paramss match {
      case first +: rem =>
        val firstIsStatic: Term.Param => Boolean = p =>
          if (isCase) isStatic(Seq.empty, p.mods) else isStaticCtorParam(p)
        first.map(
          p => ScopedTag(Seq.empty, p.name, firstIsStatic(p), p.name.pos)
        ) ++
          (for {
            pGroup <- rem
            param <- pGroup
          } yield {
            ScopedTag(
              Seq.empty,
              param.name,
              isStaticCtorParam(param),
              param.name.pos
            )
          })
      case Seq() => Nil
    }
  }

  private def isStaticCtorParam(param: Term.Param) =
    param.mods.isEmpty || isStatic(Seq.empty, param.mods)

  private def isStatic(prefix: Seq[String], mods: Seq[Mod]): Boolean =
    mods
      .collect {
        case Mod.Private(Name.Anonymous()) => true
        case Mod.Private(Term.This(Name.Anonymous())) => true
        case Mod.Private(Name.Indeterminate(name)) =>
          // DESNOTE(2017-03-21, pjrt): If the name of the private thing
          // is the parent object, then it is just private.
          if (name == "this" || prefix.contains(name))
            true
          else
            false
      }
      .headOption
      .getOrElse(false)
}
