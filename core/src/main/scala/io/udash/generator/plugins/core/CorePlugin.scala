package io.udash.generator.plugins.core

import java.io.File

import io.udash.generator.plugins._
import io.udash.generator.plugins.sbt.SBTProjectFiles
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

object CorePlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {
  override def run(settings: GeneratorSettings): GeneratorSettings = {
    settings.projectType match {
      case FrontendOnlyProject =>
        addUdashCoreDependency(settings)
        createJSApp(rootPackageInSrc(settings.rootDirectory, settings), settings)
      case StandardProject(_, shared, frontend) =>
        addUdashCoreDependency(settings)
        createJSApp(rootPackageInSrc(settings.rootDirectory.subFile(frontend), settings), settings)
    }

    settings
  }

  private def addUdashCoreDependency(settings: GeneratorSettings): Unit = {
    requireFilesExist(Seq(dependenciesScala(settings)))

    appendOnPlaceholder(dependenciesScala(settings))(DependenciesVariablesPlaceholder,
      s"""val udashCoreVersion = "${settings.udashCoreVersion}"
         |val logbackVersion = "${settings.logbackVersion}"""".stripMargin)

    appendOnPlaceholder(dependenciesScala(settings))(DependenciesCrossPlaceholder,
      s""",
         |    "io.udash" % "udash-core-shared" % udashCoreVersion""".stripMargin)

    appendOnPlaceholder(dependenciesScala(settings))(DependenciesFrontendPlaceholder,
      s""",
         |    "io.udash" %%% "udash-core-frontend" % udashCoreVersion""".stripMargin)

    appendOnPlaceholder(dependenciesScala(settings))(DependenciesBackendPlaceholder,
      s""",
         |    "ch.qos.logback" % "logback-classic" % logbackVersion""".stripMargin)
  }

  private def createJSApp(rootPackage: File, settings: GeneratorSettings): Unit = {
    val initScala = rootPackage.subFile("init.scala")
    val statesScala = rootPackage.subFile("states.scala")
    val routingRegistryDefScala = rootPackage.subFile("RoutingRegistryDef.scala")
    val statesToViewPresenterDefScala = rootPackage.subFile("StatesToViewPresenterDef.scala")
    val rootViewScala = viewsPackageInSrc(rootPackage).subFile("RootView.scala")
    val indexViewScala = viewsPackageInSrc(rootPackage).subFile("IndexView.scala")
    val errorViewScala = viewsPackageInSrc(rootPackage).subFile("ErrorView.scala")

    createDirs(Seq(viewsPackageInSrc(rootPackage)))
    createFiles(Seq(initScala, statesScala, routingRegistryDefScala, statesToViewPresenterDefScala,
                    rootViewScala, indexViewScala, errorViewScala), requireNotExists = true)

    writeFile(initScala)(
      s"""package ${settings.rootPackage.mkPackage()}
         |
         |import io.udash._
         |import io.udash.wrappers.jquery._
         |import org.scalajs.dom.{Element, document}
         |
         |import scala.scalajs.js.JSApp
         |import scala.scalajs.js.annotation.JSExport
         |
         |object Context {
         |  implicit val executionContext = scalajs.concurrent.JSExecutionContext.Implicits.queue
         |  private val routingRegistry = new RoutingRegistryDef
         |  private val viewPresenterRegistry = new StatesToViewPresenterDef
         |
         |  implicit val applicationInstance = new Application[RoutingState](routingRegistry, viewPresenterRegistry, RootState)$FrontendContextPlaceholder
         |}
         |
         |object Init extends JSApp with StrictLogging {
         |  import Context._
         |
         |  @JSExport
         |  override def main(): Unit = {
         |    jQ(document).ready((_: Element) => {
         |      val appRoot = jQ("#${settings.htmlRootId}").get(0)
         |      if (appRoot.isEmpty) {
         |        logger.error("Application root element not found! Check your index.html file!")
         |      } else {
         |        applicationInstance.run(appRoot.get)$FrontendAppInitPlaceholder
         |      }
         |    })
         |  }
         |}
         |""".stripMargin
    )

    writeFile(statesScala)(
      s"""package ${settings.rootPackage.mkPackage()}
         |
         |import io.udash._
         |
         |sealed abstract class RoutingState(val parentState: RoutingState) extends State {
         |  def url(implicit application: Application[RoutingState]): String = s"#${"${application.matchState(this).value}"}"
         |}
         |
         |case object RootState extends RoutingState(null)
         |
         |case object ErrorState extends RoutingState(RootState)
         |
         |case object IndexState extends RoutingState(RootState)$FrontendStatesRegistryPlaceholder
         |""".stripMargin
    )

    writeFile(routingRegistryDefScala)(
      s"""package ${settings.rootPackage.mkPackage()}
         |
         |import io.udash._
         |import io.udash.utils.Bidirectional
         |
         |class RoutingRegistryDef extends RoutingRegistry[RoutingState] {
         |  def matchUrl(url: Url): RoutingState =
         |    url2State.applyOrElse(url.value.stripSuffix("/"), (x: String) => ErrorState)
         |
         |  def matchState(state: RoutingState): Url =
         |    Url(state2Url.apply(state))
         |
         |  private val (url2State, state2Url) = Bidirectional[String, RoutingState] {
         |    case "" => IndexState$FrontendRoutingRegistryPlaceholder
         |  }
         |}
         |""".stripMargin
    )

    writeFile(statesToViewPresenterDefScala)(
      s"""package ${settings.rootPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}._
         |
         |class StatesToViewPresenterDef extends ViewPresenterRegistry[RoutingState] {
         |  def matchStateToResolver(state: RoutingState): ViewPresenter[_ <: RoutingState] = state match {
         |    case RootState => RootViewPresenter
         |    case IndexState => IndexViewPresenter$FrontendVPRegistryPlaceholder
         |    case _ => ErrorViewPresenter
         |  }
         |}
         |""".stripMargin
    )

    writeFile(rootViewScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}.{RootState, IndexState}
         |import org.scalajs.dom.Element
         |
         |object RootViewPresenter extends DefaultViewPresenterFactory[RootState.type](() => new RootView)
         |
         |class RootView extends View {
         |  import ${settings.rootPackage.mkPackage()}.Context._
         |  import scalatags.JsDom.all._
         |
         |  private var child: Element = div().render
         |
         |  private val content = div(
         |    a(href := IndexState.url)(h1("${settings.projectName}")),
         |    child
         |  ).render
         |
         |  override def getTemplate: Element = content
         |
         |  override def renderChild(view: View): Unit = {
         |    import io.udash.wrappers.jquery._
         |    val newChild = view.getTemplate
         |    jQ(child).replaceWith(newChild)
         |    child = newChild
         |  }
         |}
         |""".stripMargin
    )

    writeFile(indexViewScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}._
         |import org.scalajs.dom.Element
         |
         |object IndexViewPresenter extends DefaultViewPresenterFactory[IndexState.type](() => new IndexView)
         |
         |class IndexView extends View {
         |  import ${settings.rootPackage.mkPackage()}.Context._
         |  import scalatags.JsDom.all._
         |
         |  private val content = div(
         |    "Thank you for choosing Udash! Take a look at following demo pages:",
         |    ul(
         |      li(a(href := IndexState.url)("Index"))$FrontendIndexMenuPlaceholder
         |    )
         |  ).render
         |
         |  override def getTemplate: Element = content
         |
         |  override def renderChild(view: View): Unit = {}
         |}
         |""".stripMargin
    )

    writeFile(errorViewScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}.IndexState
         |import org.scalajs.dom.Element
         |
         |object ErrorViewPresenter extends DefaultViewPresenterFactory[IndexState.type](() => new ErrorView)
         |
         |class ErrorView extends View {
         |  import scalatags.JsDom.all._
         |
         |  private val content = h3(
         |    "URL not found!"
         |  ).render
         |
         |  override def getTemplate: Element = content
         |
         |  override def renderChild(view: View): Unit = {}
         |}
         |""".stripMargin
    )
  }
}
