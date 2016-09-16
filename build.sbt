name := "udash-generator"

version in ThisBuild := "0.4.0-SNAPSHOT"
organization in ThisBuild := "io.udash"
scalaVersion in ThisBuild := "2.11.8"

lazy val generator = project.in(file("."))
  .aggregate(core, cmd)
  .settings(publishArtifact := false)

lazy val core = project.in(file("core"))
  .settings(libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % Test)

lazy val cmd = project.in(file("cmd"))
  .dependsOn(core)
  .settings(
    assemblyJarName in assembly := "udash-generator.jar",
    mainClass in assembly := Some("io.udash.generator.Launcher")
  )