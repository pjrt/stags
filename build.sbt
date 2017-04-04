import java.nio.file.{Files, Paths}
import java.nio.file.attribute.{PosixFilePermission => Perm}

import scala.collection.JavaConverters._

import sbtassembly.AssemblyPlugin.defaultShellScript

lazy val install = taskKey[Unit]("install")

lazy val cli =
  (project in file("."))
    .settings(
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.12.1",

      scalacOptions ++= scalacOps,
      scalacOptions in (Compile, console) ~=
        (_.filterNot(_ == "-Ywarn-unused-import")),
      libraryDependencies ++=
        Seq(
          "com.github.scopt" %% "scopt" % "3.5.0",
          "org.scalameta" %% "scalameta" % "1.6.0",
          "org.scalatest" %% "scalatest" % "3.0.1" % "test"
        ),
      mainClass in assembly := Some("co.pjrt.stags.cli.Main"),
      assemblyJarName in assembly := s"stags-${version.value}",
      install := {
        assembly.value
        val jar = (assemblyOutputPath in assembly).value
        val target = new File(userHome + "/.local/lib/" + (assemblyJarName in assembly).value)
        val shFilePath = userHome + "/.local/bin/stags"
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
      }
    )

lazy val userHome: String = System.getProperty("user.home")

lazy val shFileContent = Def.task {
  s"""#!/bin/sh
  |java -jar $userHome/.local/lib/${(assemblyJarName in assembly).value} $$@
  """.stripMargin
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
