package io.udash.generator.configuration

import io.udash.generator._
import io.udash.generator.exceptions.InvalidConfigDecisionResponse
import io.udash.generator.plugins.PlaceholdersCleanPlugin
import io.udash.generator.plugins.core.{CoreDemosPlugin, CorePlugin}
import io.udash.generator.plugins.jetty.JettyLauncherPlugin
import io.udash.generator.plugins.rpc.{RPCDemosPlugin, RPCPlugin}
import io.udash.generator.plugins.sbt.{SBTBootstrapPlugin, SBTModulesPlugin}
import io.udash.generator.plugins.scalacss.ScalaCSSDemosPlugin

import scala.annotation.tailrec
import scala.collection.mutable

trait DecisionMaker {
  /** Should return decision with filled response option. */
  def makeDecision[T](decision: Decision[T], current: GeneratorSettings): Decision[T]
}

case class Configuration(plugins: Seq[GeneratorPlugin], settings: GeneratorSettings)

/** Creates project configuration based on `decisionMaker` responses. */
class ConfigurationBuilder(decisionMaker: DecisionMaker) {
  def build(): Configuration = {
    @tailrec
    def _build(plugins: Seq[GeneratorPlugin], settings: GeneratorSettings)(decisions: List[Decision[_]]): Configuration = {
      if (decisions.isEmpty) Configuration(plugins, settings)
      else {
        val response = decisionMaker.makeDecision(decisions.head, settings)
        val errors: Option[String] = response.validator()
        if (errors.isDefined) throw InvalidConfigDecisionResponse(errors.get)
        _build(
          plugins ++ selectPlugins(response),
          changeSettings(response, settings)
        )(selectNextDecisions(response, settings) ++ decisions.tail)
      }
    }

    def dependenciesOrder(plugins: Seq[GeneratorPlugin]): Seq[GeneratorPlugin] = {
      val visited = mutable.Set.empty[GeneratorPlugin]
      val builder = mutable.ArrayBuffer[GeneratorPlugin]()

      def visit(plugin: GeneratorPlugin): Unit =
        if (!visited.contains(plugin)) {
          visited += plugin
          plugin.dependencies.filter(plugins.contains).foreach(visit)
          builder.prepend(plugin)
        }

      plugins.foreach(visit)
      builder.reverseIterator.toSeq
    }

    val configuration = _build(Seq(), GeneratorSettings(null, false, null, null, null, null, false))(startingDecisions)
    val sortedPlugins = dependenciesOrder(configuration.plugins)
    configuration.copy(plugins = sortedPlugins)
  }

  private def selectPlugins(response: Decision[_]): Seq[GeneratorPlugin] = {
    response match {
      case ProjectTypeSelect(Some(FrontendOnlyProject)) =>
        Seq(SBTBootstrapPlugin, SBTModulesPlugin)
      case StdProjectTypeModulesSelect(Some(projectType)) =>
        Seq(SBTBootstrapPlugin, SBTModulesPlugin)
      case CreateBasicFrontendApp(Some(true)) =>
        Seq(CorePlugin)
      case CreateFrontendDemos(Some(true)) =>
        Seq(CoreDemosPlugin)
      case CreateScalaCSSDemos(Some(true)) =>
        Seq(ScalaCSSDemosPlugin)
      case CreateJettyLauncher(Some(true)) =>
        Seq(JettyLauncherPlugin)
      case CreateRPC(Some(true)) =>
        Seq(RPCPlugin)
      case CreateRPCDemos(Some(true)) =>
        Seq(RPCDemosPlugin)
      case RunGenerator(Some(true)) =>
        Seq(PlaceholdersCleanPlugin)
      case _ =>
        Seq.empty
    }
  }

  private def changeSettings(response: Decision[_], settings: GeneratorSettings): GeneratorSettings = {
    response match {
      case RootDirectory(Some(dir)) =>
        settings.copy(rootDirectory = dir)
      case ClearRootDirectory(Some(clear)) =>
        settings.copy(shouldRemoveExistingData = clear)
      case ProjectName(Some(name)) =>
        settings.copy(projectName = name)
      case Organization(Some(name)) =>
        settings.copy(organization = name)
      case RootPackage(Some(pck)) =>
        settings.copy(rootPackage = pck)
      case ProjectTypeSelect(Some(FrontendOnlyProject)) =>
        settings.copy(projectType = FrontendOnlyProject)
      case StdProjectTypeModulesSelect(Some(projectType)) =>
        settings.copy(projectType = projectType)
      case EnableJsWorkbench(Some(enable)) =>
        settings.copy(shouldEnableJsWorkbench = enable)
      case _ =>
        settings
    }
  }

  private def selectNextDecisions(response: Decision[_], settings: GeneratorSettings): List[Decision[_]] = {
    response match {
      case ProjectTypeSelect(Some(FrontendOnlyProject)) =>
        List(CreateBasicFrontendApp())
      case ProjectTypeSelect(Some(_: StandardProject)) =>
        List(StdProjectTypeModulesSelect())
      case StdProjectTypeModulesSelect(Some(projectType)) =>
        List(CreateBasicFrontendApp())
      case CreateBasicFrontendApp(Some(true)) =>
        settings.projectType match {
          case FrontendOnlyProject =>
            List(CreateFrontendDemos(), CreateScalaCSSDemos())
          case StandardProject(_, _, _) =>
            List(CreateFrontendDemos(), CreateScalaCSSDemos(), CreateJettyLauncher())
        }
      case CreateJettyLauncher(Some(true)) =>
        List(CreateRPC())
      case CreateRPC(Some(true)) =>
        List(CreateRPCDemos())
      case _ =>
        List.empty
    }
  }

  private val startingDecisions: List[Decision[_]] =
    List[Decision[_]](
      RootDirectory(),
      ClearRootDirectory(),
      ProjectName(),
      Organization(),
      RootPackage(),
      ProjectTypeSelect(),
      EnableJsWorkbench(),
      RunGenerator()
    )
}
