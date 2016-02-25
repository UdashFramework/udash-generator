package io.udash.generator.plugins.rpc

import java.io.File

import io.udash.generator.exceptions.InvalidConfiguration
import io.udash.generator.plugins._
import io.udash.generator.plugins.sbt.SBTProjectFiles
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

object RPCDemosPlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {
  override def run(settings: GeneratorSettings): GeneratorSettings = {
    val rootPck: File = settings.projectType match {
      case FrontendOnlyProject =>
        throw InvalidConfiguration("You can not add RPC into frontend only project.")
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
         |      li(a(href := $state.url)("RPC demo"))""".stripMargin)
  }

  private def createDemoView(rootPackage: File, settings: GeneratorSettings): String = {
    val statesScala = rootPackage.subFile("states.scala")
    val routingRegistryDefScala = rootPackage.subFile("RoutingRegistryDef.scala")
    val statesToViewPresenterDefScala = rootPackage.subFile("StatesToViewPresenterDef.scala")

    requireFilesExist(Seq(viewsPackageInSrc(rootPackage), statesScala, routingRegistryDefScala, statesToViewPresenterDefScala))

    val rpcDemoViewScala = viewsPackageInSrc(rootPackage).subFile("RPCDemoView.scala")
    val stateName = "RPCDemoState"

    appendOnPlaceholder(statesScala)(FrontendStatesRegistryPlaceholder,
      s"""
         |
         |case object $stateName extends RoutingState(RootState)""".stripMargin)

    appendOnPlaceholder(routingRegistryDefScala)(FrontendRoutingRegistryPlaceholder,
      s"""
         |    case "/rpc" => $stateName""".stripMargin)

    appendOnPlaceholder(statesToViewPresenterDefScala)(FrontendVPRegistryPlaceholder,
      s"""
         |    case $stateName => RPCDemoViewPresenter""".stripMargin)

    writeFile(rpcDemoViewScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}.$stateName
         |import org.scalajs.dom.Element
         |
         |import scala.util.{Success, Failure}
         |
         |case object RPCDemoViewPresenter extends DefaultViewPresenterFactory[$stateName.type](() => {
         |  import ${settings.rootPackage.mkPackage()}.Context._
         |
         |  val serverResponse = Property[String]("???")
         |  val input = Property[String]("")
         |  input.listen((value: String) => {
         |    serverRpc.hello(value).onComplete {
         |      case Success(resp) => serverResponse.set(resp)
         |      case Failure(_) => serverResponse.set("Error")
         |    }
         |  })
         |
         |  serverRpc.pushMe()
         |
         |  new RPCDemoView(input, serverResponse)
         |})
         |
         |class RPCDemoView(input: Property[String], serverResponse: Property[String]) extends View {
         |  import scalatags.JsDom.all._
         |
         |  private val content = div(
         |    div(
         |      "You can find this demo source code in: ",
         |      i("${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.RPCDemoView")
         |    ),
         |    h3("Example"),
         |    TextInput(input, placeholder := "Type your name..."),
         |    div("Server response: ", bind(serverResponse))
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
