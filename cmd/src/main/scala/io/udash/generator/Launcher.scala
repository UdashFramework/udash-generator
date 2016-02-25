package io.udash.generator

import io.udash.generator.configuration.ConfigurationBuilder

object Launcher {
  def main(args: Array[String]) {
    val generator = new Generator
    val configBuilder = new ConfigurationBuilder(new CmdDecisionMaker)
    val config = configBuilder.build()

    generator.start(config.plugins, config.settings)
  }
}
