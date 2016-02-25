package io.udash.generator.plugins.scalacss

import java.io.File

import io.udash.generator.plugins._
import io.udash.generator.plugins.sbt.SBTProjectFiles
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

object ScalaCSSDemosPlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {

  override def run(settings: GeneratorSettings): GeneratorSettings = {
    val rootPck: File = settings.projectType match {
      case FrontendOnlyProject =>
        rootPackageInSrc(settings.rootDirectory, settings)
      case StandardProject(_, shared, frontend) =>
        rootPackageInSrc(settings.rootDirectory.subFile(frontend), settings)
    }
    val stateName = createDemoStyles(rootPck, settings)
    addIndexLink(rootPck, stateName)

    settings
  }

  private def addIndexLink(rootPackage: File, state: String): Unit = {
    val indexViewScala = viewsPackageInSrc(rootPackage).subFile("IndexView.scala")
    requireFilesExist(Seq(indexViewScala))

    appendOnPlaceholder(indexViewScala)(FrontendIndexMenuPlaceholder,
      s""",
          |      li(a(href := $state.url)("ScalaCSS demo view"))""".stripMargin)
  }

  private def createDemoStyles(rootPackage: File, settings: GeneratorSettings): String = {
    val demoStylesScala = stylesPackageInSrc(rootPackage).subFile("DemoStyles.scala")
    val initScala = rootPackage.subFile("init.scala")

    val statesScala = rootPackage.subFile("states.scala")
    val routingRegistryDefScala = rootPackage.subFile("RoutingRegistryDef.scala")
    val statesToViewPresenterDefScala = rootPackage.subFile("StatesToViewPresenterDef.scala")

    val demoStylesViewScala = viewsPackageInSrc(rootPackage).subFile("DemoStylesView.scala")
    val stateName = "DemoStylesState"

    createDirs(Seq(stylesPackageInSrc(rootPackage)))
    createFiles(Seq(demoStylesScala, demoStylesViewScala), requireNotExists = true)
    requireFilesExist(Seq(dependenciesScala(settings), initScala, statesScala, routingRegistryDefScala, statesToViewPresenterDefScala))

    appendOnPlaceholder(dependenciesScala(settings))(DependenciesFrontendPlaceholder,
      s""",
         |    "com.github.japgolly.scalacss" %%% "core" % "${settings.scalaCSSVersion}",
         |    "com.github.japgolly.scalacss" %%% "ext-scalatags" % "${settings.scalaCSSVersion}"""".stripMargin)

    appendOnPlaceholder(statesScala)(FrontendStatesRegistryPlaceholder,
      s"""
         |
         |case object $stateName extends RoutingState(RootState)""".stripMargin)

    appendOnPlaceholder(routingRegistryDefScala)(FrontendRoutingRegistryPlaceholder,
      s"""
         |    case "/scalacss" => $stateName""".stripMargin)

    appendOnPlaceholder(statesToViewPresenterDefScala)(FrontendVPRegistryPlaceholder,
      s"""
         |    case $stateName => DemoStylesViewPresenter""".stripMargin)

    writeFile(demoStylesViewScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}.$stateName
         |import org.scalajs.dom.Element
         |
         |import scala.concurrent.duration.DurationInt
         |import scala.language.postfixOps
         |import scalacss.Defaults._
         |
         |case object DemoStylesViewPresenter extends DefaultViewPresenterFactory[$stateName.type](() => new DemoStylesView)
         |
         |class DemoStylesView extends View {
         |  import scalacss.Defaults._
         |  import scalacss.ScalatagsCss._
         |  import scalatags.JsDom._
         |  import scalatags.JsDom.all._
         |
         |  private val content = div(
         |    LocalStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]],
         |    div(
         |      "You can find this demo source code in: ",
         |      i("${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.DemoStylesView")
         |    ),
         |    h3("Example"),
         |    p(LocalStyles.redItalic)("Red italic text."),
         |    p(LocalStyles.obliqueOnHover)("Hover me!"),
         |    h3("Read more"),
         |    a(href := "http://japgolly.github.io/scalacss/book/")("Read more in ScalaCSS docs.")
         |  ).render
         |
         |  override def getTemplate: Element = content
         |
         |  override def renderChild(view: View): Unit = {}
         |
         |  object LocalStyles extends StyleSheet.Inline {
         |    import dsl._
         |
         |    val redItalic = style(
         |      fontStyle.italic,
         |      color.red
         |    )
         |
         |    val obliqueOnHover = style(
         |      fontStyle.normal,
         |
         |      &.hover(
         |        fontStyle.oblique
         |      )
         |    )
         |  }
         |}
         |""".stripMargin
    )

    appendOnPlaceholder(initScala)(FrontendAppInitPlaceholder,
      s"""
         |
         |import scalacss.Defaults._
         |import scalacss.ScalatagsCss._
         |import scalatags.JsDom._
         |import scalatags.JsDom.all._
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.DemoStyles
         |jQ(appRoot.get).addClass(DemoStyles.container.className.value)
         |jQ(DemoStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)""".stripMargin)

    writeFile(demoStylesScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}
         |
         |import scala.concurrent.duration.DurationInt
         |import scala.language.postfixOps
         |import scalacss.Defaults._
         |
         |object DemoStyles extends StyleSheet.Inline {
         |  import dsl._
         |
         |  val linkHoverAnimation = keyframes(
         |    (0 %%) -> keyframe(color.black),
         |    (50 %%) -> keyframe(color.red),
         |    (100 %%) -> keyframe(color.black)
         |  )
         |
         |  val container = style(
         |    width(1000 px),
         |    margin(0 px, auto),
         |
         |    unsafeChild("h1")(
         |      textDecorationStyle.unset,
         |      textDecorationLine.none
         |    ),
         |
         |    unsafeChild("a")(
         |      color.black,
         |
         |      &.hover(
         |        animationName(linkHoverAnimation),
         |        animationIterationCount.count(1),
         |        animationDuration(300 milliseconds)
         |      )
         |    )
         |  )
         |}
         |""".stripMargin)

    stateName
  }
}
