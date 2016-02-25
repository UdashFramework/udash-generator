package io.udash.generator

import io.udash.generator.utils.FileOps

class Generator extends FileOps {
  /**
    * Starts project generation process.
    *
    * @param plugins Sequence of generator plugins, which will be fired.
    * @param settings Initial project settings.
    */
  def start(plugins: Seq[GeneratorPlugin], settings: GeneratorSettings): GeneratorSettings = {
    if (settings.shouldRemoveExistingData) removeFileOrDir(settings.rootDirectory)
    settings.rootDirectory.mkdirs()
    plugins.foldLeft(settings)((settings: GeneratorSettings, plugin: GeneratorPlugin) => plugin.run(settings))
  }
}