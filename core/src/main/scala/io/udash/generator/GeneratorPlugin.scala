package io.udash.generator

import io.udash.generator.utils.FileOps

/** Part of generation chain. */
trait GeneratorPlugin extends FileOps {
  /** Starts plugins work with current project settings.
    * @return Project settings which will be passed to next plugin in generator sequence. */
  def run(settings: GeneratorSettings): GeneratorSettings

  /** Plugin should start after `dependencies` plugins if they were selected. */
  val dependencies: Seq[GeneratorPlugin] = Seq()
}
