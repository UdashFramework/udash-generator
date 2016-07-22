package io.udash.generator

import java.io.File

/**
  * Basic project configuration.
  *
  * @param rootDirectory Root directory of whole project.
  * @param shouldRemoveExistingData If `true`, generator will remove whole data from rootDirectory.
  * @param rootPackage Root package of sources.
  * @param projectType Project modules configuration.
  * @param organization Organization name.
  * @param projectName Project name.
  */
case class GeneratorSettings(rootDirectory: File,
                             shouldRemoveExistingData: Boolean,
                             projectName: String,
                             organization: String,
                             projectType: ProjectType,
                             rootPackage: Seq[String],
                             shouldEnableJsWorkbench: Boolean) {
  /** Root package of views in frontend. */
  def viewsSubPackage: Seq[String] = Seq("views")
  /** Root package of styles in frontend. */
  def stylesSubPackage: Seq[String] = Seq("styles")

  def scalaVersion: String = "2.11.8"
  def sbtVersion: String = "0.13.11"
  def scalaJSVersion: String = "0.6.10"
  def scalaCSSVersion: String = "0.4.1"
  def udashVersion: String = "0.3.0"
  def udashJQueryVersion: String = "1.0.0"
  def jettyVersion: String = "9.3.8.v20160314"
  def logbackVersion: String = "1.1.3"

  /** Application HTML root element id */
  def htmlRootId: String = "application"

  /** Generated JS file with application code name (dev). */
  def frontendImplFastJs: String = "frontend-impl-fast.js"
  /** Generated JS file with application code name (prod). */
  def frontendImplJs: String = "frontend-impl.js"
  /** Generated JS file with dependencies code name (dev). */
  def frontendDepsFastJs: String = "frontend-deps-fast.js"
  /** Generated JS file with dependencies code name (prod). */
  def frontendDepsJs: String = "frontend-deps.js"
  /** Generated JS file with app launcher name (dev). */
  def frontendInitJs: String = "frontend-init.js"

  /** Udash DevGuide root URL. */
  def udashDevGuide: String = "http://guide.udash.io/"

  /** Assets images */
  def imageResourcePath = "/images/"
  def assetsImages = Seq("icon_avsystem.png", "icon_github.png", "icon_stackoverflow.png", "udash_logo.png", "udash_logo_m.png")
}

sealed trait ProjectType

/** Project does not contain submodules, it's only frontend application and everything is compiled to JavaScript. */
case object FrontendOnlyProject extends ProjectType

/**
  * Standard Udash project with three submodules:
  *
  * @param backend - module compiled to JVM name
  * @param shared - module compiled to JS and JVM name
  * @param frontend - module compiled to JS name
  */
case class StandardProject(backend: String, shared: String, frontend: String) extends ProjectType
