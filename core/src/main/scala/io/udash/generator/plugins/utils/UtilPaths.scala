package io.udash.generator.plugins.utils

import java.io.File

import io.udash.generator.GeneratorSettings
import io.udash.generator.utils._

trait UtilPaths {
  def src(module: File): File =
    module.subFile(srcPathPart)
  def testSrc(module: File): File =
    module.subFile(testSrcPathPart)
  def rootPackageInSrc(module: File, settings: GeneratorSettings): File =
    module.subFile(Seq(srcPathPart, packagePathPart(settings)).mkString(File.separator))
  def rootPackageInTestSrc(module: File, settings: GeneratorSettings): File =
    module.subFile(Seq(testSrcPathPart, packagePathPart(settings)).mkString(File.separator))

  private val srcPathPart = Seq("src", "main", "scala").mkString(File.separator)
  private val testSrcPathPart = Seq("src", "test", "scala").mkString(File.separator)
  private def packagePathPart(settings: GeneratorSettings) = settings.rootPackage.mkString(File.separator)
}
