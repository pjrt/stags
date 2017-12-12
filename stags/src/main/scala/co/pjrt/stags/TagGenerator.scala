package co.pjrt.stags

import java.io.File

import scala.meta._

import co.pjrt.stags.paths.Path

object TagGenerator {

  /**
   * Given a Scalameta Source, generate a sequence of [[Tag]]s for it
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
        val packageScope = generatePackageScope(obj.ref)
        obj.stats.flatMap(tagsForStatement(packageScope, _, false))
      case st =>
        tagsForStatement(Scope.empty, st, false)
    }
  }

  private def generatePackageScope(ref: Term.Ref): Scope = {
    def loop(r: Term, acc: Seq[String]): Seq[String] =
      r match {
        case name: Term.Name     => acc :+ name.value
        case select: Term.Select => loop(select.qual, acc :+ select.name.value)
        case _                   => acc
      }
    Scope(loop(ref, Seq.empty))
  }

  private def tagsForStatement(
      scope: Scope,
      child: Stat,
      forceChildrenToStatic: Boolean
    ): Seq[ScopedTag] = {

    def getStatic(mods: Seq[Mod]) =
      forceChildrenToStatic || isStatic(scope, mods)

    child match {
      // DESNOTE(2017-08-09, pjrt): This means we have reached another package
      // statement in the same file. Though rare, it is possible. What should
      // happen in this case is that this package statement should merge
      // with the package statement above
      case pkg: Pkg =>
        val thisPkgScope = generatePackageScope(pkg.ref)
        val newPackageScope = thisPkgScope.packageScope ++ scope.packageScope
        val newScope = Scope(newPackageScope, scope.localScope)
        pkg.stats.flatMap(tagsForStatement(newScope, _, false))
      // DESNOTE(2017-03-15, pjrt) There doesn't seem to be a way to
      // access common fields in Defn (mods, name, etc), though looking here
      // https://github.com/scalameta/scalameta/blob/master/scalameta/trees/src/main/scala/scala/meta/Trees.scala#L336
      // it looks like there should be a way.
      case d: Defn.Def =>
        Seq(tag(scope, d, getStatic(d.mods)))
      case d: Decl.Def => Seq(tag(scope, d, getStatic(d.mods)))
      case d: Defn.Val =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic))
      case d: Decl.Val =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic))
      case d: Defn.Var =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic))
      case d: Decl.Var =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic))
      case d: Defn.Type => Seq(tag(scope, d, getStatic(d.mods)))
      case d: Decl.Type => Seq(tag(scope, d, getStatic(d.mods)))

      case d: Defn.Object =>
        val newScope = scope.addLocal(d.name)
        val static = getStatic(d.mods)
        tag(scope, d, static) +:
          d.templ.stats.flatMap(s => tagsForStatement(newScope, s, static))
      case d: Pkg.Object =>
        val newScope = scope.addLocal(d.name)
        val static = getStatic(d.mods)
        tag(scope, d, static) +:
          d.templ.stats.flatMap(s => tagsForStatement(newScope, s, static))

      case d: Defn.Trait =>
        val static = getStatic(d.mods)
        tag(scope, d, static) +:
          d.templ.stats.flatMap(s => tagsForStatement(Scope.empty, s, static))
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

        val static = getStatic(d.mods)
        (tag(scope, d, static) +: ctorParamTags) ++
          d.templ.stats.flatMap(s => tagsForStatement(Scope.empty, s, static))

      case _ => Seq.empty
    }
  }

  private def getFromPats(
      scope: Scope,
      mods: Seq[Mod],
      pat: Pat,
      staticParent: Boolean
    ): Seq[ScopedTag] = {

    val static = staticParent || isStatic(scope, mods)
    def getFromPat(p: Pat) = getFromPats(scope, mods, p, staticParent)
    pat match {
      case p: Pat.Var      => Seq(tag(scope, p, static))
      case Pat.Typed(p, _) => getFromPat(p)
      case Pat.Tuple(args) => args.flatMap(getFromPat)
      case Pat.Extract(_, pats) =>
        pats.flatMap(getFromPat)
      case Pat.ExtractInfix(lhs, _, pats) =>
        getFromPat(lhs) ++ pats.flatMap(getFromPat)
      case Pat.Wildcard() | Pat.SeqWildcard() =>
        // DESNOTE(2017-05-15, pjrt): underscored vals are inaccesable
        Seq.empty
      case Pat.Bind(lhs, rhs) =>
        getFromPat(lhs)
      case _: Lit =>
        // DESNOTE(2017-07-14, pjrt): This is for patterns like `val 1 = x`
        // For those, we should not generate tags (of course)
        Seq.empty
    }
  }

  private def tag(scope: Scope, term: Member, static: Boolean) = {

    ScopedTag.fromMember(scope, static, term)
  }

  private def tag(term: Member, static: Boolean) = {

    ScopedTag.fromMember(Scope.empty, static, term)
  }

  // When we are dealing with implicit classes, the parameter oughts to be
  // static. Even though it can be accessed from the outside, it is very
  // unlikely to ever be.
  private def tagForImplicitClassParam(param: Term.Param) = tag(param, true)

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
          if (isCase) isStatic(Scope.empty, p.mods)
          else isStaticCtorParam(p)
        first.map(
          p => tag(p, firstIsStatic(p))
        ) ++
          (for {
            pGroup <- rem
            param <- pGroup
          } yield {
            tag(param, isStaticCtorParam(param))
          })
      case Seq() => Nil
    }
  }

  private def isStaticCtorParam(param: Term.Param) =
    param.mods.isEmpty || isStatic(Scope.empty, param.mods)

  private def isStatic(prefix: Scope, mods: Seq[Mod]): Boolean =
    mods
      .collect {
        case Mod.Private(Name.Anonymous())            => true
        case Mod.Private(Term.This(Name.Anonymous())) => true
        case Mod.Private(Name.Indeterminate(name))    =>
          // DESNOTE(2017-03-21, pjrt): If the name of the private thing
          // is the parent object, then it is just private.
          if (name == "this" || prefix.localContains(name))
            true
          else
            false
      }
      .headOption
      .getOrElse(false)
}
