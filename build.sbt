import sbtassembly.AssemblyPlugin.defaultShellScript


lazy val cli =
  (project in file("."))
    .settings(
      scalaVersion := "2.12.1",

      scalacOptions ++= Seq(
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
        "-Xfuture",
        "-Ywarn-unused-import",
        "-Ywarn-dead-code",
        "-Ypartial-unification"
      ),
    libraryDependencies ++=
      Seq(
        "com.github.scopt" %% "scopt" % "3.5.0",
        "org.scalameta" %% "scalameta" % "1.6.0",
        "org.scalameta" %% "contrib" % "1.6.0",
        "org.scalatest" %% "scalatest" % "3.0.1"
      ),
    mainClass in assembly := Some("co.pjrt.sctags.cli.Main"),
    assemblyJarName in assembly := s"stags-${version.value}"
  )

