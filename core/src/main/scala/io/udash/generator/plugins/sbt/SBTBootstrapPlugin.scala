package io.udash.generator.plugins.sbt

import io.udash.generator.plugins.{DependenciesPlaceholder, UdashBuildPlaceholder}
import io.udash.generator.utils._
import io.udash.generator.{GeneratorPlugin, GeneratorSettings}

/**
  * Creates basic SBT project:<br/>
  * * build.sbt with basic project settings<br/>
  * * project/build.properties with SBT version<br/>
  * * project/plugins.sbt with ScalaJS plugin<br/>
  * * project/UdashBuild.scala with custom tasks<br/>
  * * project/Dependencies.scala with dependencies<br/>
  */
object SBTBootstrapPlugin extends GeneratorPlugin with SBTProjectFiles {
  override def run(settings: GeneratorSettings): GeneratorSettings = {
    createDirs(Seq(projectDir(settings)), requireNotExists = true)
    createFiles(Seq(buildSbt(settings), buildProperties(settings), pluginsSbt(settings),
                    udashBuildScala(settings), dependenciesScala(settings)))

    val importJsWorkbench = if (settings.shouldEnableJsWorkbench) "import com.lihaoyi.workbench.Plugin._" else ""
    val addJsWorkbenchPlugin = if (settings.shouldEnableJsWorkbench) s"""addSbtPlugin("com.lihaoyi" % "workbench" % "0.2.3")""" else ""

    writeFile(buildSbt(settings))(
      s"""${importJsWorkbench}
         |
         |name := "${settings.projectName}"
         |
         |version in ThisBuild := "0.1.0-SNAPSHOT"
         |scalaVersion in ThisBuild := "${settings.scalaVersion}"
         |organization in ThisBuild := "${settings.organization}"
         |crossPaths in ThisBuild := false
         |scalacOptions in ThisBuild ++= Seq(
         |  "-feature",
         |  "-deprecation",
         |  "-unchecked",
         |  "-language:implicitConversions",
         |  "-language:existentials",
         |  "-language:dynamics",
         |  "-Xfuture",
         |  "-Xfatal-warnings",
         |  "-Xlint:_,-missing-interpolator,-adapted-args"
         |)
         |
         |""".stripMargin
    )

    writeFile(buildProperties(settings))(
      s"sbt.version = ${settings.sbtVersion}")

    writeFile(pluginsSbt(settings))(
      s"""logLevel := Level.Warn
         |addSbtPlugin("org.scala-js" % "sbt-scalajs" % "${settings.scalaJSVersion}")
         |
         |${addJsWorkbenchPlugin}
         |
         |""".stripMargin)

    writeFile(udashBuildScala(settings))(
      s"""import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
         |import sbt.Keys._
         |import sbt._
         |
         |object UdashBuild extends Build {$UdashBuildPlaceholder}
         |
         |""".stripMargin)

    writeFile(dependenciesScala(settings))(
      s"""import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
         |import sbt._
         |
         |object Dependencies extends Build {$DependenciesPlaceholder}
         |
         |""".stripMargin)

    settings
  }
}
