package io.udash.generator.plugins

import java.io.File

import io.udash.generator.{GeneratorPlugin, GeneratorSettings}

/**
  * Removes all placeholders from generated project.
  * Placeholders look like: /*<<udash-generator-*>>*/
  */
object PlaceholdersCleanPlugin extends GeneratorPlugin {
  override def run(settings: GeneratorSettings): GeneratorSettings = {
    cleanDirectory(settings.rootDirectory)
    settings
  }

  private def cleanDirectory(dir: File): Unit =
    dir.listFiles()
      .foreach(f =>
        if (f.isDirectory) cleanDirectory(f)
        else cleanFile(f)
      )

  private def cleanFile(file: File): Unit = {
    removeFromFile(file)("/\\*<<udash-generator.*?>>\\*/")
    // Clear leading comma in dependencies
    replaceInFile(file)("\\(\\s*,", "(")
  }
}
