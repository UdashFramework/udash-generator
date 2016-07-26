package io.udash.generator

import java.io.File

import io.udash.generator.configuration._
import io.udash.generator.exceptions.InvalidConfigDecisionResponse

import scala.io.StdIn

class CmdDecisionMaker extends DecisionMaker {
  private val yesAnswers = Seq("yes", "y", "true", "t")

  override def makeDecision[T](d: Decision[T], current: GeneratorSettings): Decision[T] = {
    try {
      val response: Decision[T] = d match {
        case decision@RootDirectory(_) =>
          RootDirectory(Some(askForFile("Project root directory", decision.default)))
        case decision@ClearRootDirectory(_) =>
          ClearRootDirectory(Some(askForBoolean("Clear root directory", decision.default)))
        case decision@ProjectName(_) =>
          ProjectName(Some(askForString("Project name", decision.default)))
        case decision@Organization(_) =>
          Organization(Some(askForString("Organization", decision.default)))
        case decision@RootPackage(_) =>
          RootPackage(Some(askForPackage("Root package", current.organization.split('.'))))
        case decision@ProjectTypeSelect(_) =>
          ProjectTypeSelect(Some(askForSelection("Project type", decision.default, decision.options)))
        case decision@StdProjectTypeModulesSelect(_) =>
          val backend = askForString("Backend module name", decision.default.backend)
          val shared = askForString("Shared module name", decision.default.shared)
          val frontend = askForString("Frontend module name", decision.default.frontend)
          StdProjectTypeModulesSelect(Some(StandardProject(backend, shared, frontend)))
        case decision@CreateBasicFrontendApp(_) =>
          CreateBasicFrontendApp(Some(askForBoolean("Create basic frontend application", decision.default)))
        case decision@CreateFrontendDemos(_) =>
          CreateFrontendDemos(Some(askForBoolean("Create frontend demo views", decision.default)))
        case decision@CreateScalaCSSDemos(_) =>
          CreateScalaCSSDemos(Some(askForBoolean("Create ScalaCSS demo views", decision.default)))
        case decision@CreateJettyLauncher(_) =>
          CreateJettyLauncher(Some(askForBoolean("Create Jetty launcher", decision.default)))
        case decision@CreateRPC(_) =>
          CreateRPC(Some(askForBoolean("Create RPC communication layer", decision.default)))
        case decision@CreateRPCDemos(_) =>
          CreateRPCDemos(Some(askForBoolean("Create RPC communication layer demos", decision.default)))
        case decision@EnableJsWorkbench(_) =>
          EnableJsWorkbench(Some(askForBoolean("Enable JsWorkbench usage", decision.default)))
        case decision@RunGenerator(_) =>
          RunGenerator(Some(askForBoolean("Start generation", decision.default)))
      }
      for (errors <- response.validator()) throw InvalidConfigDecisionResponse(errors)
      response
    } catch {
      case InvalidConfigDecisionResponse(ex) =>
        println(ex)
        makeDecision(d, current)
      case _: Exception =>
        makeDecision(d, current)
    }
  }

  private def ask[T](prompt: String, default: T)(converter: String => T): T = {
    val response = StdIn.readLine(prompt).trim
    if (response.isEmpty) default else converter(response)
  }
  
  private def askWithDefault[T](prompt: String, default: T)(converter: String => T): T =
    ask(s"$prompt [$default]: ", default)(converter)

  private def askForString(prompt: String, default: String): String =
    askWithDefault(prompt, default)(s => s)

  private def askForBoolean(prompt: String, default: Boolean): Boolean =
    askWithDefault(prompt, default)(r => yesAnswers.contains(r.toLowerCase))

  private def askForPackage(prompt: String, default: Seq[String]): Seq[String] =
    ask(s"$prompt [${default.mkString(".")}]: ", default)(s => s.split("\\."))

  private def askForFile(prompt: String, default: File): File =
    ask(s"$prompt [${default.getAbsolutePath}]: ", default)(r => new File(r))

  private def askForSelection[T](prompt: String, default: T, options: Seq[T]): T = {
    val optionsPresentation = options.zipWithIndex.map{
      case decision@(opt, idx) => s"  ${idx+1}. $opt \n"
    }.mkString
    ask(s"$prompt [${options.indexOf(default) + 1}]:\n${optionsPresentation}Select: ", default)(r => options(Integer.parseInt(r) - 1))
  }
}
