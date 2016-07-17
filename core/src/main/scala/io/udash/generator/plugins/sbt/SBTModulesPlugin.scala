package io.udash.generator.plugins.sbt

import java.io.File

import io.udash.generator.exceptions.FileCreationError
import io.udash.generator.plugins._
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

/**
  * Prepares SBT modules configuration.
  */
object SBTModulesPlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {

  override val dependencies = Seq(SBTBootstrapPlugin)

  override def run(settings: GeneratorSettings): GeneratorSettings = {
    settings.projectType match {
      case FrontendOnlyProject =>
        generateFrontendOnlyProject(settings)
      case StandardProject(backend, shared, frontend) =>
        generateStandardProject(
          settings.rootDirectory.subFile(backend),
          settings.rootDirectory.subFile(shared),
          settings.rootDirectory.subFile(frontend), settings)
    }

    settings
  }

  private def scalajsWorkbenchSettings(settings: GeneratorSettings) =
    s""".settings(workbenchSettings:_*)
       |  .settings(
       |    bootSnippet := "${settings.rootPackage.mkPackage()}.Init().main();",
       |    updatedJS := {
       |      var files: List[String] = Nil
       |      ((crossTarget in Compile).value / StaticFilesDir ** "*.js").get.foreach {
       |        (x: File) =>
       |          streams.value.log.info("workbench: Checking " + x.getName)
       |          FileFunction.cached(streams.value.cacheDirectory / x.getName, FilesInfo.lastModified, FilesInfo.lastModified) {
       |            (f: Set[File]) =>
       |              val fsPath = f.head.getAbsolutePath.drop(new File("").getAbsolutePath.length)
       |              files = "http://localhost:12345/" + fsPath :: files
       |              f
       |          }(Set(x))
       |      }
       |      files
       |    },
       |    //// use either refreshBrowsers OR updateBrowsers
       |    // refreshBrowsers <<= refreshBrowsers triggeredBy (compileStatics in Compile)
       |    updateBrowsers <<= updateBrowsers triggeredBy (compileStatics in Compile)
       |  )
       |""".stripMargin

  /**
    * Creates modules dirs:<br/>
    * * src/main/assets<br/>
    * * src/main/assets/fonts<br/>
    * * src/main/assets/images<br/>
    * * src/main/assets/index.dev.html<br/>
    * * src/main/assets/index.prod.html<br/>
    * * src/main/scala/{rootPackage}<br/>
    * * src/test/scala/{rootPackage}<br/>
    * and appends `build.sbt` modules config with dependencies config in `project/Dependencies.scala`.
    */
  private def generateFrontendOnlyProject(settings: GeneratorSettings): Unit = {
    createModulesDirs(Seq(settings.rootDirectory), settings)
    createFrontendExtraDirs(settings.rootDirectory, settings)

    requireFilesExist(Seq(buildSbt(settings), projectDir(settings), udashBuildScala(settings), dependenciesScala(settings)))
    generateFrontendTasks(udashBuildScala(settings), indexDevHtml(settings.rootDirectory), indexProdHtml(settings.rootDirectory))

    val frontendModuleName = wrapValName(settings.projectName)
    val depsName = wrapValName("deps")
    val depsJSName = wrapValName("depsJS")

    appendFile(buildSbt(settings))(
      s"""val $frontendModuleName = project.in(file(".")).enablePlugins(ScalaJSPlugin)
          |  .settings(
          |    libraryDependencies ++= $depsName.value,
          |    jsDependencies ++= $depsJSName.value,
          |    persistLauncher in Compile := true,
          |
          |    compile <<= (compile in Compile).dependsOn(compileStatics),
          |    compileStatics := {
          |      IO.copyDirectory(sourceDirectory.value / "main/assets/fonts", crossTarget.value / StaticFilesDir / WebContent / "assets/fonts")
          |      IO.copyDirectory(sourceDirectory.value / "main/assets/images", crossTarget.value / StaticFilesDir / WebContent / "assets/images")
          |      compileStaticsForRelease.value
          |      (crossTarget.value / StaticFilesDir).***.get
          |    },
          |
          |    artifactPath in(Compile, fastOptJS) :=
          |      (crossTarget in(Compile, fastOptJS)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendImplFastJs}",
          |    artifactPath in(Compile, fullOptJS) :=
          |      (crossTarget in(Compile, fullOptJS)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendImplJs}",
          |    artifactPath in(Compile, packageJSDependencies) :=
          |      (crossTarget in(Compile, packageJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendDepsFastJs}",
          |    artifactPath in(Compile, packageMinifiedJSDependencies) :=
          |      (crossTarget in(Compile, packageMinifiedJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendDepsJs}",
          |    artifactPath in(Compile, packageScalaJSLauncher) :=
          |      (crossTarget in(Compile, packageScalaJSLauncher)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendInitJs}"$FrontendSettingsPlaceholder
          |  )${scalajsWorkbenchSettings(settings)}
          |  $FrontendModulePlaceholder
          |
          |""".stripMargin)

    appendOnPlaceholder(dependenciesScala(settings))(DependenciesPlaceholder,
      s"""
         |  $DependenciesVariablesPlaceholder
         |
         |  val $depsName = Def.setting(Seq[ModuleID]($DependenciesFrontendPlaceholder
         |  ))
         |
         |  val $depsJSName = Def.setting(Seq[org.scalajs.sbtplugin.JSModuleID]($DependenciesFrontendJSPlaceholder
         |  ))
         |""".stripMargin
    )
  }

  /**
    * Creates modules dirs:<br/>
    * * {module}/src/main/scala/{rootPackage}<br/>
    * * {module}/src/test/scala/{rootPackage}<br/>
    * extra in frontend:<br/>
    * * {module}/src/main/assets<br/>
    * * {module}/src/main/assets/fonts<br/>
    * * {module}/src/main/assets/images<br/>
    * * {module}/src/main/assets/index.dev.html<br/>
    * * {module}/src/main/assets/index.prod.html<br/>
    * and appends `build.sbt` modules config with dependencies config in `project/Dependencies.scala`.
    */
  private def generateStandardProject(backend: File, shared: File, frontend: File, settings: GeneratorSettings): Unit = {
    createModulesDirs(Seq(backend, shared, frontend), settings)
    createFrontendExtraDirs(frontend, settings)

    requireFilesExist(Seq(buildSbt(settings), projectDir(settings), udashBuildScala(settings), dependenciesScala(settings)))
    generateFrontendTasks(udashBuildScala(settings), indexDevHtml(frontend), indexProdHtml(frontend))

    val rootModuleName = wrapValName(settings.projectName)
    val backendModuleName = wrapValName(backend.getName)
    val frontendModuleName = wrapValName(frontend.getName)
    val sharedModuleName = wrapValName(shared.getName)
    val sharedJSModuleName = wrapValName(shared.getName + "JS")
    val sharedJVMModuleName = wrapValName(shared.getName + "JVM")

    val crossDepsName = wrapValName("crossDeps")
    val backendDepsName = wrapValName("backendDeps")
    val frontendDepsName = wrapValName("frontendDeps")
    val frontendJSDepsName = wrapValName("frontendJSDeps")

    appendFile(buildSbt(settings))(
      s"""def crossLibs(configuration: Configuration) =
         |  libraryDependencies ++= $crossDepsName.value.map(_ % configuration)
         |
         |lazy val $rootModuleName = project.in(file("."))
         |  .aggregate($sharedJSModuleName, $sharedJVMModuleName, $frontendModuleName, $backendModuleName)
         |  .dependsOn($backendModuleName)
         |  .settings(
         |    publishArtifact := false$RootSettingsPlaceholder
         |  )$RootModulePlaceholder
         |
         |lazy val $sharedModuleName = crossProject.crossType(CrossType.Pure).in(file("${shared.getName}"))
         |  .settings(
         |    crossLibs(Provided)$SharedSettingsPlaceholder
         |  )$SharedModulePlaceholder
         |
         |lazy val $sharedJVMModuleName = $sharedModuleName.jvm$SharedJVMModulePlaceholder
         |lazy val $sharedJSModuleName = $sharedModuleName.js$SharedJSModulePlaceholder
         |
         |lazy val $backendModuleName = project.in(file("${backend.getName}"))
         |  .dependsOn($sharedJVMModuleName)
         |  .settings(
         |    libraryDependencies ++= $backendDepsName.value,
         |    crossLibs(Compile)$BackendSettingsPlaceholder
         |  )$BackendModulePlaceholder
         |
         |lazy val $frontendModuleName = project.in(file("${frontend.getName}")).enablePlugins(ScalaJSPlugin)
         |  .dependsOn($sharedJSModuleName)
         |  .settings(
         |    libraryDependencies ++= $frontendDepsName.value,
         |    crossLibs(Compile),
         |    jsDependencies ++= $frontendJSDepsName.value,
         |    persistLauncher in Compile := true,
         |
         |    compile <<= (compile in Compile),
         |    compileStatics := {
         |      IO.copyDirectory(sourceDirectory.value / "main/assets/fonts", crossTarget.value / StaticFilesDir / WebContent / "assets/fonts")
         |      IO.copyDirectory(sourceDirectory.value / "main/assets/images", crossTarget.value / StaticFilesDir / WebContent / "assets/images")
         |      compileStaticsForRelease.value
         |      (crossTarget.value / StaticFilesDir).***.get
         |    },
         |    compileStatics <<= compileStatics.dependsOn(compile in Compile),
         |
         |    artifactPath in(Compile, fastOptJS) :=
         |      (crossTarget in(Compile, fastOptJS)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendImplFastJs}",
         |    artifactPath in(Compile, fullOptJS) :=
         |      (crossTarget in(Compile, fullOptJS)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendImplJs}",
         |    artifactPath in(Compile, packageJSDependencies) :=
         |      (crossTarget in(Compile, packageJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendDepsFastJs}",
         |    artifactPath in(Compile, packageMinifiedJSDependencies) :=
         |      (crossTarget in(Compile, packageMinifiedJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendDepsJs}",
         |    artifactPath in(Compile, packageScalaJSLauncher) :=
         |      (crossTarget in(Compile, packageScalaJSLauncher)).value / StaticFilesDir / WebContent / "scripts" / "${settings.frontendInitJs}"$FrontendSettingsPlaceholder
         |  )${scalajsWorkbenchSettings(settings)}
         |  $FrontendModulePlaceholder
         |
         |""".stripMargin)

    appendOnPlaceholder(dependenciesScala(settings))(DependenciesPlaceholder,
      s"""
         |  $DependenciesVariablesPlaceholder
         |
         |  val $crossDepsName = Def.setting(Seq[ModuleID]($DependenciesCrossPlaceholder
         |  ))
         |
         |  val $frontendDepsName = Def.setting(Seq[ModuleID]($DependenciesFrontendPlaceholder
         |  ))
         |
         |  val $frontendJSDepsName = Def.setting(Seq[org.scalajs.sbtplugin.JSModuleID]($DependenciesFrontendJSPlaceholder
         |  ))
         |
         |  val $backendDepsName = Def.setting(Seq[ModuleID]($DependenciesBackendPlaceholder
         |  ))
         |""".stripMargin
    )
  }

  private def createModulesDirs(modules: Seq[File], settings: GeneratorSettings): Unit = {
    modules.foreach((modulePath: File) => {
      val module = modulePath
      if (modulePath != settings.rootDirectory && !module.mkdir()) throw FileCreationError(module.toString)
      createDirs(Seq(rootPackageInSrc(module, settings), rootPackageInTestSrc(module, settings)))
    })
  }

  private def createFrontendExtraDirs(frontend: File, settings: GeneratorSettings): Unit = {
    createDirs(Seq(images(frontend), fonts(frontend)))

    val indexDev: File = indexDevHtml(frontend)
    val indexProd: File = indexProdHtml(frontend)

    createFiles(Seq(indexDev, indexProd), requireNotExists = true)

    writeFile(indexDev)(
      s"""<!DOCTYPE html>
          |<html>
          |<head lang="en">
          |    <meta charset="UTF-8">
          |    <title>${settings.projectName} - development</title>
          |
          |    <script src="http://localhost:12345/frontend/target/UdashStatic/WebContent/scripts/${settings.frontendDepsFastJs}"></script>
          |    <script src="http://localhost:12345/frontend/target/UdashStatic/WebContent/scripts/${settings.frontendImplFastJs}"></script>
          |    <script src="http://localhost:12345/frontend/target/UdashStatic/WebContent/scripts/${settings.frontendInitJs}"></script>
          |    <script src="http://localhost:12345/workbench.js"></script>
          |
          |    $HTMLHeadPlaceholder
          |</head>
          |<body>
          |  <div id="${settings.htmlRootId}"></div>
          |</body>
          |</html>
          |""".stripMargin)

    writeFile(indexProd)(
      s"""<!DOCTYPE html>
          |<html>
          |<head lang="en">
          |    <meta charset="UTF-8">
          |    <title>${settings.projectName}</title>
          |
          |    <script src="scripts/${settings.frontendDepsJs}"></script>
          |    <script src="scripts/${settings.frontendImplJs}"></script>
          |    <script src="scripts/${settings.frontendInitJs}"></script>
          |    $HTMLHeadPlaceholder
          |</head>
          |<body>
          |  <div id="${settings.htmlRootId}"></div>
          |</body>
          |</html>
          |""".stripMargin)
  }

  private def generateFrontendTasks(udashBuildScala: File, indexDevHtml: File, indexProdHtml: File): Unit = {
    appendOnPlaceholder(udashBuildScala)(UdashBuildPlaceholder,
      s"""
         |  val StaticFilesDir = "UdashStatic"
         |  val WebContent = "WebContent"
         |
         |  def copyIndex(file: File, to: File) = {
         |    val newFile = Path(to.toPath.toString + "/index.html")
         |    IO.copyFile(file, newFile.asFile)
         |  }
         |
         |  val compileStatics = taskKey[Seq[File]]("Frontend static files manager.")
         |
         |  val compileStaticsForRelease = Def.taskDyn {
         |    val outDir = crossTarget.value / StaticFilesDir / WebContent
         |    if (!isSnapshot.value) {
         |      Def.task {
         |        val indexFile = sourceDirectory.value / "main/assets/${indexProdHtml.getName}"
         |        copyIndex(indexFile, outDir)
         |        (fullOptJS in Compile).value
         |        (packageMinifiedJSDependencies in Compile).value
         |        (packageScalaJSLauncher in Compile).value
         |      }
         |    } else {
         |      Def.task {
         |        val indexFile = sourceDirectory.value / "main/assets/${indexDevHtml.getName}"
         |        copyIndex(indexFile, outDir)
         |        (fastOptJS in Compile).value
         |        (packageJSDependencies in Compile).value
         |        (packageScalaJSLauncher in Compile).value
         |      }
         |    }
         |  }
         |""".stripMargin)
  }

  //TODO: wrap only when its necessary
  private def wrapValName(name: String): String =
    if (name.contains("-")) s"`$name`"
    else name
}
