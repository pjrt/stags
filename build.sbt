import java.nio.file.{Files, Paths}
import java.nio.file.attribute.{PosixFilePermission => Perm}

import scala.collection.JavaConverters._

import ReleaseTransformations._

lazy val dist = taskKey[Unit]("dist")
lazy val distClean = taskKey[Unit]("distClean")
lazy val distLocation = settingKey[String]("distLocation")

lazy val commonSettings =
  Seq(
    organization := "co.pjrt",
    scalaVersion := "2.12.2",
    scalacOptions := scalacOps,
    scalacOptions in (Compile, console) ~=
      (_.filterNot(_ == "-Ywarn-unused-import")),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    useGpg := true,
    releaseProcess := releaseProcessDef
  ) ++ publishInfo

lazy val stags =
  (project in file("stags"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "org.scalameta" %% "scalameta" % "1.8.0",
      publishSetting
    )

lazy val cli =
  (project in file("cli"))
    .enablePlugins(BuildInfoPlugin)
    .dependsOn(stags % "compile->compile;test->test")
    .settings(commonSettings: _*)
    .settings(
      name := "stags-cli",
      libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0",
      mainClass in assembly := Some("co.pjrt.stags.cli.Main"),
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "co.pjrt.stags.cli.build",
      assemblyJarName in assembly := s"stags-${version.value}",
      distLocation := (baseDirectory in assembly).value + "/dist/",
      dist := {
        assembly.value
        val jar = (assemblyOutputPath in assembly).value
        val distLoc = distLocation.value
        val libLoc = distLoc + "lib/" + (assemblyJarName in assembly).value
        val target = new File(libLoc)
        val shFilePath = distLoc + "bin/stags"
        IO.copyFile(jar, target)
        IO.write(new File(shFilePath), shFileContent.value.getBytes)
        val perms =
          Set(
            Perm.OWNER_EXECUTE,
            Perm.OWNER_READ,
            Perm.OWNER_WRITE,
            Perm.GROUP_READ,
            Perm.OTHERS_READ
          ).asJava
        Files.setPosixFilePermissions(Paths.get(shFilePath), perms)
      },
      distClean := {
        clean.value
        IO.delete(new File("dist"))
      },
      publishSetting
    )

lazy val root = (project in file("."))
  .aggregate(stags, cli)
  .settings(publishInfo: _*)
  .settings(
    // Don't publish useless root artifacts
    packagedArtifacts := Map.empty
  )

def publishSetting =
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
    )

lazy val shFileContent = Def.task {
  s"""#!/bin/sh
  |java -jar ${(baseDirectory in assembly).value}/dist/lib/${(assemblyJarName in assembly).value} $$@""".stripMargin
}

lazy val scalacOps = Seq(
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

lazy val publishInfo =
  Seq(
    sonatypeProfileName := "co.pjrt",

    // To sync with Maven central, you need to supply the following information:
    publishMavenStyle := true,

    // License of your choice
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
        id="pjrt",
        name="Pedro J Rodriguez Tavarez",
        email="pedro@pjrt.co",
        url=url("http://www.pjrt.co/"))
    )
  )

def releaseProcessDef = Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)
