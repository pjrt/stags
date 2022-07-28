import sbt.Keys.*
import sbt.*
import com.typesafe.sbt.pgp.PgpKeys.*
import xerial.sbt.Sonatype.SonatypeKeys.*
import sbtrelease.ReleasePlugin.autoImport.*
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*

object ProjectPlugin extends AutoPlugin {

  override def trigger = allRequirements

  private lazy val scalacOps = Seq(
    "-encoding",
    "utf8", // Specify character encoding used by source files.
    "-Xsource:3", // Treat compiler input as Scala source for the specified version.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:deprecation", // Emit warning and location for usages of deprecated APIs.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Wunused:nowarn", // Ensure that a `@nowarn` annotation actually suppresses a warning.
    "-Wdead-code", // Warn when dead code is identified.
    "-Wextra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Wnumeric-widen", // Warn when numerics are widened.
    "-Xlint:implicit-recursion", // Warn when an implicit resolves to an enclosing self-definition
    "-Wunused:implicits", // Warn if an implicit parameter is unused.
    "-Wunused:explicits", // Warn if an explicit parameter is unused.
    "-Wunused:imports", // Warn if an import selector is not referenced.
    "-Wunused:locals", // Warn if a local definition is unused.
    "-Wunused:params", // Warn if a value parameter is unused.
    "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates", // Warn if a private member is unused.
    "-Wvalue-discard", // Warn when non-Unit expression results are unused.
    "-Vimplicits" // Enables the tek/splain features to make the compiler print implicit resolution chains when no implicit value can be found
  )

  override def buildSettings =
    Seq(
      organization := "co.pjrt",
      scalaVersion := "2.13.8",
      releaseVersionFile := baseDirectory.value / "version.sbt",
      sonatypeProfileName := "co.pjrt",
      resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots")
      ),
      // To sync with Maven central, you need to supply the following information:
      publishMavenStyle := true,
      licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
      homepage := Some(url("https://github.com/pjrt/stags")),
      scmInfo := Some(
        ScmInfo(
          url("https://github.com/pjrt/stags"),
          "scm:git@github.com:pjrt/stags.git"
        )
      ),
      developers := List(
        Developer(
          id = "pjrt",
          name = "Pedro J Rodriguez Tavarez",
          email = "pedro@pjrt.co",
          url = url("http://www.pjrt.co/")
        )
      ),
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        ReleaseStep(action = Command.process("stags/publishSigned", _)),
        ReleaseStep(action = Command.process("cli/publishSigned", _)),
        setNextVersion,
        commitNextVersion,
        ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
        pushChanges
      )
    )

  override def projectSettings =
    Seq(
      scalacOptions := scalacOps,
      scalacOptions in (Compile, console) ~=
        (_.filterNot(_ == "-Ywarn-unused-import")),
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % "test"
    )
}
