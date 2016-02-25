package io.udash.generator.plugins.sbt

import io.udash.generator.GeneratorSettings
import io.udash.generator.utils._

trait SBTProjectFiles {
  def buildSbt(settings: GeneratorSettings) = settings.rootDirectory.subFile("build.sbt")
  def projectDir(settings: GeneratorSettings) = settings.rootDirectory.subFile("project")
  def buildProperties(settings: GeneratorSettings) = projectDir(settings).subFile("build.properties")
  def pluginsSbt(settings: GeneratorSettings) = projectDir(settings).subFile("plugins.sbt")
  def udashBuildScala(settings: GeneratorSettings) = projectDir(settings).subFile("UdashBuild.scala")
  def dependenciesScala(settings: GeneratorSettings) = projectDir(settings).subFile("Dependencies.scala")
}
