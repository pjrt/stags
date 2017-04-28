import java.nio.file.{Files, Paths}
import java.nio.file.attribute.{PosixFilePermission => Perm}

import scala.collection.JavaConverters._

lazy val dist = taskKey[Unit]("dist")
lazy val distClean = taskKey[Unit]("distClean")
lazy val distLocation = settingKey[String]("distLocation")

lazy val Benchmark = config("bench") extend Test

lazy val libVersion = "0.1"

lazy val commonSettings =
  Seq(
    organization := "co.pjrt",
    scalaVersion := "2.12.2",
    scalacOptions := scalacOps,
    version := libVersion,
    scalacOptions in (Compile, console) ~=
      (_.filterNot(_ == "-Ywarn-unused-import")),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
    )
  )

lazy val root =
  (project in file("."))
    .aggregate(stags, cli)

lazy val stags =
  (project in file("stags"))
    .settings(commonSettings:_*)
    .settings(
      libraryDependencies ++=
        Seq(
          "org.scalameta" %% "scalameta" % "1.6.0",
          "com.storm-enroute" %% "scalameter" % "0.8.2" % "bench"
        ),
      testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
      parallelExecution in Benchmark := false,
      logBuffered := false)
    .configs(Benchmark)
    .settings(inConfig(Benchmark)(Defaults.testSettings):_*)

lazy val cli =
  (project in file("cli"))
    .dependsOn(stags % "compile->compile;test->test")
    .settings(commonSettings:_*)
    .settings(
      libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0",
      mainClass in assembly := Some("co.pjrt.stags.cli.Main"),
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
      }
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
