package io.udash.generator.plugins.utils

import java.io.File

import io.udash.generator.utils._

trait FrontendPaths { utils: UtilPaths =>
  def assets(module: File): File =
    module.subFile(assetsPathPart)
  def images(module: File): File =
    module.subFile(imagesPathPart)
  def fonts(module: File): File =
    module.subFile(fontsPathPart)
  def indexDevHtml(module: File): File =
    new File(s"$module${File.separator}$assetsPathPart${File.separator}index.dev.html")
  def indexProdHtml(module: File): File =
    new File(s"$module${File.separator}$assetsPathPart${File.separator}index.prod.html")

  def viewsPackageInSrc(rootPackage: File): File =
    rootPackage.subFile("views")

  def stylesPackageInSrc(rootPackage: File): File =
    rootPackage.subFile("styles")

  private val assetsPathPart = Seq("src", "main", "assets").mkString(File.separator)
  private val imagesPathPart = Seq(assetsPathPart, "images").mkString(File.separator)
  private val fontsPathPart = Seq(assetsPathPart, "fonts").mkString(File.separator)
}
