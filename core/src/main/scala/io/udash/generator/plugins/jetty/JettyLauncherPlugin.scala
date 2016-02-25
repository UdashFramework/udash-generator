package io.udash.generator.plugins.jetty

import java.io.File

import io.udash.generator.exceptions.InvalidConfiguration
import io.udash.generator.plugins._
import io.udash.generator.plugins.sbt.SBTProjectFiles
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

object JettyLauncherPlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {
  override def run(settings: GeneratorSettings): GeneratorSettings = {
    settings.projectType match {
      case FrontendOnlyProject =>
        throw InvalidConfiguration("You can not add Jetty launcher into frontend only project.")
      case StandardProject(backend, _, frontend) =>
        updateSBTConfig(settings, frontend)
        createJettyServer(rootPackageInSrc(settings.rootDirectory.subFile(backend), settings), settings, backend)
    }

    settings
  }

  private def updateSBTConfig(settings: GeneratorSettings, frontendModuleName: String): Unit = {
    val sbtConfigFile = buildSbt(settings)
    val sbtDepsFile = dependenciesScala(settings)
    val udashBuildFile = udashBuildScala(settings)

    requireFilesExist(Seq(sbtConfigFile, sbtDepsFile, udashBuildFile))

    appendOnPlaceholder(sbtConfigFile)(RootSettingsPlaceholder,
      s""",
         |    mainClass in Compile := Some("${settings.rootPackage.mkPackage()}.Launcher")""".stripMargin)

    appendOnPlaceholder(sbtConfigFile)(BackendSettingsPlaceholder,
      s""",
         |
         |    compile <<= (compile in Compile),
         |    (compile in Compile) <<= (compile in Compile).dependsOn(copyStatics),
         |    copyStatics := IO.copyDirectory((crossTarget in $frontendModuleName).value / StaticFilesDir, (target in Compile).value / StaticFilesDir),
         |    copyStatics <<= copyStatics.dependsOn(compileStatics in $frontendModuleName),
         |
         |    mappings in (Compile, packageBin) ++= {
         |      copyStatics.value
         |      ((target in Compile).value / StaticFilesDir).***.get map { file =>
         |        file -> file.getAbsolutePath.stripPrefix((target in Compile).value.getAbsolutePath)
         |      }
         |    },
         |
         |    watchSources ++= (sourceDirectory in $frontendModuleName).value.***.get""".stripMargin)

    appendOnPlaceholder(sbtDepsFile)(DependenciesVariablesPlaceholder,
      s"""
         |  val jettyVersion = "${settings.jettyVersion}"""".stripMargin)

    appendOnPlaceholder(sbtDepsFile)(DependenciesBackendPlaceholder,
      s""",
         |    "org.eclipse.jetty" % "jetty-server" % jettyVersion,
         |    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion""".stripMargin)

    appendOnPlaceholder(udashBuildFile)(UdashBuildPlaceholder,
      s"""
         |  val copyStatics = taskKey[Unit]("Copy frontend static files into backend target.")""".stripMargin)
  }

  private def createJettyServer(rootPackage: File, settings: GeneratorSettings, backendModuleName: String): Unit = {
    val jettyDir = "jetty"
    val jettyPackage = rootPackage.subFile(jettyDir)
    val appServerScala = jettyPackage.subFile("ApplicationServer.scala")
    val launcherScala = rootPackage.subFile("Launcher.scala")

    requireFilesExist(Seq(rootPackage))
    createDirs(Seq(jettyPackage))
    createFiles(Seq(appServerScala, launcherScala))

    writeFile(appServerScala)(
      s"""package ${settings.rootPackage.mkPackage()}.$jettyDir
         |
         |import org.eclipse.jetty.server.Server
         |import org.eclipse.jetty.server.handler.gzip.GzipHandler
         |import org.eclipse.jetty.server.session.SessionHandler
         |import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler, ServletHolder}
         |
         |class ApplicationServer(val port: Int, resourceBase: String) {
         |  private val server = new Server(port)
         |  private val contextHandler = new ServletContextHandler
         |
         |  contextHandler.setSessionHandler(new SessionHandler)
         |  contextHandler.setGzipHandler(new GzipHandler)
         |  server.setHandler(contextHandler)
         |
         |  def start() = server.start()
         |
         |  def stop() = server.stop()
         |
         |  private val appHolder = {
         |    val appHolder = new ServletHolder(new DefaultServlet)
         |    appHolder.setAsyncSupported(true)
         |    appHolder.setInitParameter("resourceBase", resourceBase)
         |    appHolder
         |  }
         |  contextHandler.addServlet(appHolder, "/*")$BackendAppServerPlaceholder
         |}
         |
       """.stripMargin
    )

    writeFile(launcherScala)(
      s"""package ${settings.rootPackage.mkPackage()}
         |
         |import ${settings.rootPackage.mkPackage()}.$jettyDir.ApplicationServer
         |
         |object Launcher {
         |  def main(args: Array[String]): Unit = {
         |    val server = new ApplicationServer(8080, "$backendModuleName/target/UdashStatic/WebContent")
         |    server.start()
         |  }
         |}
         |
       """.stripMargin
    )
  }
}
