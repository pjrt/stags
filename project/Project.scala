import sbt.Keys._
import sbt._
import com.typesafe.sbt.pgp.PgpKeys._
import xerial.sbt.Sonatype.SonatypeKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

object ProjectPlugin extends AutoPlugin {

  override def trigger = allRequirements

  private lazy val scalacOps = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused-import",
    "-Xfuture",
    "-Ywarn-dead-code",
    "-Ypartial-unification"
  )

  override def buildSettings =
    Seq(
      organization := "co.pjrt",
      scalaVersion := "2.12.4",
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
        releaseStepCommand("stags/publishSigned"),
        releaseStepCommand("cli/publishSigned"),
        setNextVersion,
        commitNextVersion,
        releaseStepCommand("sonatypeReleaseAll"),
        pushChanges
      )
    )

  override def projectSettings =
    Seq(
      scalacOptions := scalacOps,
      scalacOptions in (Compile, console) ~=
        (_.filterNot(_ == "-Ywarn-unused-import")),
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
    )
}
