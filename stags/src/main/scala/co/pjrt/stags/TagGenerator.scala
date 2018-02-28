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
        Seq(tagForMember(scope, d, getStatic(d.mods)))
      case d: Defn.Macro =>
        Seq(tagForMember(scope, d, getStatic(d.mods)))
      case d: Decl.Def => Seq(tagForMember(scope, d, getStatic(d.mods)))
      case d: Defn.Val =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic, d))
      case d: Decl.Val =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic, d))
      case d: Defn.Var =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic, d))
      case d: Decl.Var =>
        d.pats.flatMap(getFromPats(scope, d.mods, _, forceChildrenToStatic, d))
      case d: Defn.Type => Seq(tagForMember(scope, d, getStatic(d.mods)))
      case d: Decl.Type => Seq(tagForMember(scope, d, getStatic(d.mods)))

      case d: Defn.Object =>
        val newScope = scope.addLocal(d.name)
        val static = getStatic(d.mods)
        tagForMember(scope, d, static) +:
          d.templ.stats.flatMap(s => tagsForStatement(newScope, s, static))
      case d: Pkg.Object =>
        val newScope = scope.addLocal(d.name)
        val static = getStatic(d.mods)
        tagForMember(scope, d, static) +:
          d.templ.stats.flatMap(s => tagsForStatement(newScope, s, static))

      case d: Defn.Trait =>
        val static = getStatic(d.mods)
        tagForMember(scope, d, static) +:
          d.templ.stats.flatMap(s => tagsForStatement(Scope.empty, s, static))
      case d: Defn.Class =>
        val ctorParamTags: Seq[ScopedTag] =
          if (d.isImplicitClass)
            // DESNOTE(2017-04-05, pjrt) This can only possibly have a single
            // element (due to how implicit classes work). However, parsing
            // of other cases is valid, though they would fail to compile. Just
            // to be in the safe side, flatten and map instead of _.head.head
            d.ctor.paramss.flatten.map(tagForImplicitClassParam(d, _))
          else
            tagsForCtorParams(d, d.isCaseClass, d.ctor.paramss)

        val static = getStatic(d.mods)
        (tagForMember(scope, d, static) +: ctorParamTags) ++
          d.templ.stats.flatMap(s => tagsForStatement(Scope.empty, s, static))

      case _ => Seq.empty
    }
  }

  private def getFromPats(
      scope: Scope,
      mods: Seq[Mod],
      pat: Pat,
      staticParent: Boolean,
      parent: Tree
    ): Seq[ScopedTag] = {

    val static = staticParent || isStatic(scope, mods)
    def getFromPat(p: Pat) = getFromPats(scope, mods, p, staticParent, parent)

    pat match {
      case p: Pat.Var      => Seq(patTag(scope, parent, p, static))
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
      case _ =>
        Console.err.println(s"Error matching $pat in line ${parent.syntax}")
        Seq.empty
    }
  }

  /**
   * generate a kind of the member.
   *
   * d: "data type" (case class)
   * c: class
   * f: function (def)
   * v: vals
   * b: vars (which are "bad")
   * i: "interface" (trait)
   * m: macros
   * a: type "alias"
   * po: package object
   *
   * m: a ctor param
   *
   * For declarations (ie: undefined members in traits), a `u` is prepended
   */
  private def generateKind(t: Tree): String =
    t match {
      case c: Defn.Class  => if (c.isCaseClass) "d" else "c"
      case _: Defn.Object => "o"
      case _: Defn.Def    => "f"
      case _: Defn.Val    => "v"
      case _: Defn.Var    => "b"
      case _: Defn.Trait  => "i"
      case _: Defn.Macro  => "m"
      case _: Defn.Type   => "a"

      case _: Decl.Def  => "uf"
      case _: Decl.Val  => "uv"
      case _: Decl.Var  => "ub"
      case _: Decl.Type => "ua"

      case _: Pkg.Object => "po"
      case p: Term.Param => "m"
    }

  private def tagForMember(
      scope: Scope,
      member: Member,
      static: Boolean
    ): ScopedTag = {

    val tagAddress = AddressGen.addrForTree(member, member.name)
    val tokenName = member.name.toString
    val kind = generateKind(member)

    ScopedTag(scope, tokenName, static, tagAddress, kind)
  }

  private def patTag(
      scope: Scope,
      parent: Tree,
      pat: Pat.Var,
      static: Boolean
    ): ScopedTag = {

    val addr = AddressGen.addrForTree(parent, pat.name)
    // DESNOTE(2018-02-15, pjrt): For pats, we use the parent's kind (val, var).
    val kind = generateKind(parent)
    ScopedTag(scope, pat.name.toString, static, addr, kind)
  }

  private def tagForCtor(
      parent: Tree,
      param: Term.Param,
      static: Boolean
    ): ScopedTag = {

    val addr = AddressGen.addrForTree(parent, param.name)
    val kind = generateKind(param)
    ScopedTag(Scope.empty, param.name.value, static, addr, kind)
  }

  // When we are dealing with implicit classes, the parameter oughts to be
  // static. Even though it can be accessed from the outside, it is very
  // unlikely to ever be.
  private def tagForImplicitClassParam(parent: Tree, param: Term.Param) =
    tagForCtor(parent, param, true)

  // When generating tags for Ctors of classes we need to see if it is a case
  // class. If it is, then params IN THE FIRST PARAM GROUP with no mods are
  // NOT static. Other params are still static.
  private def tagsForCtorParams(
      parent: Tree,
      isCase: Boolean,
      paramss: Seq[Seq[Term.Param]]
    ): Seq[ScopedTag] = {
    paramss match {
      case first +: rem =>
        val firstIsStatic: Term.Param => Boolean = p =>
          if (isCase) isStatic(Scope.empty, p.mods)
          else isStaticCtorParam(p)
        first.map(
          p => tagForCtor(parent, p, firstIsStatic(p))
        ) ++
          (for {
            pGroup <- rem
            param <- pGroup
          } yield {
            tagForCtor(parent, param, isStaticCtorParam(param))
          })
      case Seq() => Nil
    }
  }

  private def isStaticCtorParam(param: Term.Param) =
    param.mods.isEmpty || isStatic(Scope.empty, param.mods)

  // DESNOTE(2018-01-12, pjrt): Set of cases when we set a tag to static (making
  // it lower priority when doing vim searches).
  //
  // 1. Implicits: These are rarely accessed via an identifier (instead being
  //    used by the implicit system).
  // 2. Things marked by a plain `private` or `private[this]`
  // 3. Things marked by `private[x]` where `x` is an encompasing scope defined
  //    in the same file as the thing (ie: the thing can only be accessed from
  //    the file it is defined).
  private def isStatic(prefix: Scope, mods: Seq[Mod]): Boolean =
    mods
      .collect {
        case Mod.Implicit()                           => true
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

private object AddressGen {

  /**
   * Generate an address for the given tree and location
   *
   * The tree MUST be un-modified, otherwise the original syntax gets destroyed
   * and an address cannot be created.
   *
   * The tagName is used to find the line of the syntax that we care abour (we
   * don't care about annotatons above the tag, nor about any existing body.
   *
   * {{{
   * @someAnno
   * @someOtherAnoo def x: Int = {
   *   someCode
   * }
   * }}}
   *
   * In the example able, we only take `@someOtherAnoo def x: Int = {` as the
   * address. Note that multi-line statements will get cut off (the tag will
   * work regardless).
   *
   * The tagname is also used to determine the location of the `\zs` for better
   * cursor location.
   */
  def addrForTree(tree: Tree, tagName: Name): String = {

    // DESNOTE(2017-12-21, pjrt): This should always return 0 or more, otherwise
    // scalameta wouldn't have parsed it.
    val lineWithTagName = tagName.pos.startLine - tree.pos.startLine
    val line = tree.tokens.syntax.lines.toList(lineWithTagName).trim

    val name = tagName.value
    val replacement = s"\\\\zs$name".replaceAllLiterally("$", "\\$")
    val nameW = s"\\b\\Q$name\\E\\b" // wrap $name with regex quote \Q\E; and word bound \b\b
    val search = line.replaceFirst(nameW, replacement)
    s"/$search/"
  }
}
