import java.nio.file.{Files, Paths}
import java.nio.file.attribute.{PosixFilePermission => Perm}

import scala.collection.JavaConverters.*

import ReleaseTransformations.*

lazy val stags =
  (project in file("stags"))
    .settings(
      libraryDependencies += "org.scalameta" %% "scalameta" % "4.5.13"
    )

lazy val cli =
  (project in file("cli"))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging, GraalVMNativeImagePlugin)
    .dependsOn(stags % "compile->compile;test->test")
    .settings(
      name := "stags-cli",
      name in GraalVMNativeImage := "stags",
      libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0",
      libraryDependencies += "com.martiansoftware" % "nailgun-server" % "0.9.1",
      mainClass in assembly := Some("co.pjrt.stags.cli.Main"),
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "co.pjrt.stags.cli.build",
      assemblyJarName in assembly := s"stags-${version.value}",
      graalVMNativeImageOptions := Seq("--no-fallback")
    )

lazy val root = (project in file("."))
  .aggregate(stags, cli)
  .settings(
    // Don't publish useless root artifacts
    packagedArtifacts := Map.empty
  )
