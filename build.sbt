scalaVersion := "2.12.1"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ywarn-unused-import",
  "-Ywarn-dead-code"
)
libraryDependencies ++=
  Seq(
    "org.scalameta" %% "scalameta" % "1.6.0",
    "org.scalameta" %% "contrib" % "1.6.0",
    "org.scalatest" %% "scalatest" % "3.0.1"
  )
