package io.udash.generator.plugins.core

import java.io.File

import io.udash.generator.plugins._
import io.udash.generator.plugins.sbt.SBTProjectFiles
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

object CoreDemosPlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {

  override val dependencies = Seq(CorePlugin)

  override def run(settings: GeneratorSettings): GeneratorSettings = {
    val rootPck: File = settings.projectType match {
      case FrontendOnlyProject =>
        rootPackageInSrc(settings.rootDirectory, settings)
      case StandardProject(_, shared, frontend) =>
        rootPackageInSrc(settings.rootDirectory.subFile(frontend), settings)
    }
    val stateName = createDemoView(rootPck, settings)
    addIndexLink(rootPck, stateName)

    settings
  }

  private def addIndexLink(rootPackage: File, state: String): Unit = {
    val indexViewScala = viewsPackageInSrc(rootPackage).subFile("IndexView.scala")
    requireFilesExist(Seq(indexViewScala))

    appendOnPlaceholder(indexViewScala)(FrontendIndexMenuPlaceholder,
      s""",
         |      li(a(${FrontendStylesLinkBlackPlaceholder}href := $state().url)("Binding demo")),
         |      li(a(${FrontendStylesLinkBlackPlaceholder}href := $state("From index").url)("Binding demo with URL argument"))""".stripMargin)
  }

  private def createDemoView(rootPackage: File, settings: GeneratorSettings): String = {
    val statesScala = rootPackage.subFile("states.scala")
    val routingRegistryDefScala = rootPackage.subFile("RoutingRegistryDef.scala")
    val statesToViewPresenterDefScala = rootPackage.subFile("StatesToViewPresenterDef.scala")

    val bindingDemoViewScala = viewsPackageInSrc(rootPackage).subFile("BindingDemoView.scala")
    val stateName = "BindingDemoState"

    requireFilesExist(Seq(viewsPackageInSrc(rootPackage), statesScala, routingRegistryDefScala, statesToViewPresenterDefScala))
    createFiles(Seq(bindingDemoViewScala), requireNotExists = true)

    appendOnPlaceholder(statesScala)(FrontendStatesRegistryPlaceholder,
      s"""
         |
         |case class $stateName(urlArg: String = "") extends RoutingState(RootState)""".stripMargin)

    appendOnPlaceholder(routingRegistryDefScala)(FrontendRoutingRegistryPlaceholder,
      s"""
         |    case "/binding" => $stateName("")
         |    case "/binding" /:/ arg => $stateName(arg)""".stripMargin)

    appendOnPlaceholder(statesToViewPresenterDefScala)(FrontendVPRegistryPlaceholder,
      s"""
         |    case $stateName(urlArg) => BindingDemoViewPresenter(urlArg)""".stripMargin)

    writeFile(bindingDemoViewScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}.$stateName
         |import org.scalajs.dom.Element$FrontendImportsPlaceholder
         |
         |case class BindingDemoViewPresenter(urlArg: String) extends DefaultViewPresenterFactory[$stateName](() => {
         |  import ${settings.rootPackage.mkPackage()}.Context._
         |
         |  val model = Property[String](urlArg)
         |  new BindingDemoView(model)
         |})
         |
         |class BindingDemoView(model: Property[String]) extends View {
         |  import scalatags.JsDom.all._
         |
         |  private val content = div(
         |    h2(
         |      "You can find this demo source code in: ",
         |      i("${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.BindingDemoView")
         |    ),
         |    h3("Example"),
         |    TextInput(model, placeholder := "Type something..."),
         |    p("You typed: ", bind(model)),
         |    h3("Read more"),
         |    a$FrontendStylesLinkBlackPlaceholder(href := "http://guide.udash.io/#/frontend/bindings", target := "_blank")("Read more in Udash Guide.")
         |  ).render
         |
         |  override def getTemplate: Element = content
         |
         |  override def renderChild(view: View): Unit = {}
         |}
         |""".stripMargin
    )

    stateName
  }
}
