package io.udash.generator.plugins.scalacss

import java.io.File
import javax.imageio.ImageIO

import io.udash.generator.plugins._
import io.udash.generator.plugins.core.CoreDemosPlugin
import io.udash.generator.plugins.rpc.RPCDemosPlugin
import io.udash.generator.plugins.sbt.SBTProjectFiles
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

object ScalaCSSDemosPlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {

  val stateName = "DemoStylesState"
  override val dependencies = Seq(RPCDemosPlugin, CoreDemosPlugin)

  override def run(settings: GeneratorSettings): GeneratorSettings = {
    var rootPck: File = null
    var frontendDir: File = null
    var imagesDir: File = null

    settings.projectType match {
      case FrontendOnlyProject =>
        rootPck = rootPackageInSrc(settings.rootDirectory, settings)
        frontendDir = settings.rootDirectory
        imagesDir = images(settings.rootDirectory)
      case StandardProject(_, shared, frontend) =>
        rootPck = rootPackageInSrc(settings.rootDirectory.subFile(frontend), settings)
        frontendDir = settings.rootDirectory.subFile(frontend)
        imagesDir = images(settings.rootDirectory.subFile(frontend))
    }
    addIndexLink(rootPck, stateName)
    createDemoStyles(rootPck, settings)

    createImages(imagesDir, settings)
    prepareHtml(frontendDir, settings)

    settings
  }

  private def addIndexLink(rootPackage: File, state: String): Unit = {
    val indexViewScala = viewsPackageInSrc(rootPackage).subFile("IndexView.scala")
    requireFilesExist(Seq(indexViewScala))

    appendOnPlaceholder(indexViewScala)(FrontendIndexMenuPlaceholder,
      s""",
          |      li(a(${FrontendStylesLinkBlackPlaceholder}href := $state.url)("ScalaCSS demo view"))""".stripMargin)
  }

  private def prepareHtml(frontendDirectory: File, settings: GeneratorSettings): Unit = {
    val indexDev: File = indexDevHtml(frontendDirectory)
    val indexProd: File = indexProdHtml(frontendDirectory)

    requireFilesExist(Seq(indexDev, indexProd))

    appendOnPlaceholder(indexDev)(HTMLHeadPlaceholder,
      s"""
          |<meta name="viewport" content="width=device-width, initial-scale=1">
          |<meta http-equiv="X-UA-Compatible" content="IE=9" />
          |
          |<script src="https://use.typekit.net/ysu6xld.js"></script>
          |<script>try{Typekit.load({ async: true });}catch(e){}</script>""".stripMargin)

    appendOnPlaceholder(indexProd)(HTMLHeadPlaceholder,
      s"""
          |<meta name="viewport" content="width=device-width, initial-scale=1">
          |<meta http-equiv="X-UA-Compatible" content="IE=9" />
          |
          |<script src="https://use.typekit.net/ysu6xld.js"></script>
          |<script>try{Typekit.load({ async: true });}catch(e){}</script>""".stripMargin)

  }

  private def createImages(imagesDirectory: File, settings: GeneratorSettings): Unit = {
    createDirs(Seq(imagesDirectory))

    settings.assetsImages.foreach(filename => {
      val src = getClass.getResource(settings.imageResourcePath + filename)
      val target = imagesDirectory.subFile(filename)

      val image = ImageIO.read(src)
      ImageIO.write(image, filename.split("\\.").last, target)
    })
  }

  private def createDemoStyles(rootPackage: File, settings: GeneratorSettings): Unit = {
    val globalStylesScala = stylesPackageInSrc(rootPackage).subFile("GlobalStyles.scala")
    val demoStylesScala = stylesPackageInSrc(rootPackage).subFile("DemoStyles.scala")

    val stylesConstantsPackage = stylesPackageInSrc(rootPackage).subFile("constants")
    val stylesConstantsScala = stylesConstantsPackage.subFile("StyleConstants.scala")

    val stylesFontsPackage = stylesPackageInSrc(rootPackage).subFile("fonts")
    val stylesFontsScala = stylesFontsPackage.subFile("UdashFonts.scala")

    val stylesPartialsPackage = stylesPackageInSrc(rootPackage).subFile("partials")
    val stylesHeaderScala = stylesPartialsPackage.subFile("Header.scala")
    val stylesFooterScala = stylesPartialsPackage.subFile("FooterStyles.scala")

    val stylesUtilsPackage = stylesPackageInSrc(rootPackage).subFile("utils")
    val stylesMediaQueriesScala = stylesUtilsPackage.subFile("MediaQueries.scala")
    val stylesStyleUtilsScala = stylesUtilsPackage.subFile("StyleUtils.scala")

    val configPackage = rootPackage.subFile("config")
    val configScala = configPackage.subFile("ExternalUrls.scala")

    val initScala = rootPackage.subFile("init.scala")

    val statesScala = rootPackage.subFile("states.scala")
    val rootViewScala = viewsPackageInSrc(rootPackage).subFile("RootView.scala")
    val indexViewScala = viewsPackageInSrc(rootPackage).subFile("IndexView.scala")
    val bindingDemoViewScala = viewsPackageInSrc(rootPackage).subFile("BindingDemoView.scala")
    val routingRegistryDefScala = rootPackage.subFile("RoutingRegistryDef.scala")
    val statesToViewPresenterDefScala = rootPackage.subFile("StatesToViewPresenterDef.scala")
    val rpcDemoViewScala = viewsPackageInSrc(rootPackage).subFile("RPCDemoView.scala")

    val demoStylesViewScala = viewsPackageInSrc(rootPackage).subFile("DemoStylesView.scala")

    val componentsPackage = viewsPackageInSrc(rootPackage).subFile("components")
    val imageFactoryScala = componentsPackage.subFile("ImageFactory.scala")
    val headerScala = componentsPackage.subFile("Header.scala")
    val footerScala = componentsPackage.subFile("Footer.scala")

    createDirs(Seq(stylesPackageInSrc(rootPackage), configPackage, componentsPackage, stylesConstantsPackage, stylesFontsPackage, stylesPartialsPackage, stylesUtilsPackage))
    createFiles(Seq(globalStylesScala, demoStylesScala, demoStylesViewScala, configScala, imageFactoryScala, headerScala, footerScala, stylesConstantsScala, stylesFontsScala, stylesHeaderScala, stylesFooterScala, stylesMediaQueriesScala, stylesStyleUtilsScala), requireNotExists = true)
    requireFilesExist(Seq(dependenciesScala(settings), initScala, statesScala, routingRegistryDefScala, statesToViewPresenterDefScala, bindingDemoViewScala, indexViewScala, rootViewScala))

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

    appendOnPlaceholder(bindingDemoViewScala)(FrontendImportsPlaceholder,
      s"""
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.DemoStyles
         |import scalacss.ScalatagsCss._""".stripMargin)

    appendOnPlaceholder(bindingDemoViewScala)(FrontendStylesLinkBlackPlaceholder,
      s"""(DemoStyles.underlineLinkBlack)""".stripMargin)

    appendOnPlaceholder(rootViewScala)(FrontendImportsPlaceholder,
      s"""
         |import ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.${componentsPackage.getName}.{Footer, Header}
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.{DemoStyles, GlobalStyles}
         |import scalacss.ScalatagsCss._""".stripMargin)

    appendOnPlaceholder(rootViewScala)(FrontendStyledHeaderPlaceholder,
      s"""Header.getTemplate,""".stripMargin)

    appendOnPlaceholder(rootViewScala)(FrontendStyledFooterPlaceholder,
      s"""
         |,Footer.getTemplate""".stripMargin)

    appendOnPlaceholder(rootViewScala)(FrontendStylesMainPlaceholder,
      s"""(GlobalStyles.main)""".stripMargin)

    appendOnPlaceholder(rootViewScala)(FrontendStylesBodyPlaceHolder,
      s"""(GlobalStyles.body)""".stripMargin)

    appendOnPlaceholder(rootViewScala)(FrontendStylesLinkBlackPlaceholder,
      s"""(DemoStyles.underlineLinkBlack)""".stripMargin)

    appendOnPlaceholder(indexViewScala)(FrontendStylesStepsListPlaceholder,
      s"""(DemoStyles.stepsList)""".stripMargin)

    appendOnPlaceholder(indexViewScala)(FrontendStylesLinkBlackPlaceholder,
      s"""DemoStyles.underlineLinkBlack, """.stripMargin)

    appendOnPlaceholder(indexViewScala)(FrontendImportsPlaceholder,
      s"""
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.{DemoStyles, GlobalStyles}
         |import scalacss.ScalatagsCss._""".stripMargin)

    if (rpcDemoViewScala.exists()) appendOnPlaceholder(rpcDemoViewScala)(FrontendImportsPlaceholder,
      s"""
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.DemoStyles
         |import scalacss.ScalatagsCss._""".stripMargin)

    if (rpcDemoViewScala.exists()) appendOnPlaceholder(rpcDemoViewScala)(FrontendStylesLinkBlackPlaceholder,
      s"""(DemoStyles.underlineLinkBlack)""".stripMargin)

    writeFile(demoStylesViewScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}
         |
         |import io.udash._
         |import ${settings.rootPackage.mkPackage()}.$stateName
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.DemoStyles
         |import org.scalajs.dom.Element
         |
         |import scala.language.postfixOps
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
         |    h2(
         |      "You can find this demo source code in: ",
         |      i("${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.DemoStylesView")
         |    ),
         |    h3("Example"),
         |    p(LocalStyles.redItalic)("Red italic text."),
         |    p(LocalStyles.obliqueOnHover)("Hover me!"),
         |    h3("Read more"),
         |    ul(
         |      li(
         |        a(DemoStyles.underlineLinkBlack)(href := "${settings.udashDevGuide}#/frontend/templates", target := "_blank")("Read more in Udash Guide.")
         |      ),
         |      li(
         |       a(DemoStyles.underlineLinkBlack)(href := "https://japgolly.github.io/scalacss/book/", target := "_blank")("Read more in ScalaCSS docs.")
         |     )
         |    )
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
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.GlobalStyles
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.DemoStyles
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.partials.FooterStyles
         |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.partials.HeaderStyles
         |jQ(GlobalStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)
         |jQ(DemoStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)
         |jQ(FooterStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)
         |jQ(HeaderStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)""".stripMargin)

    writeFile(globalStylesScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}
          |
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesConstantsPackage.getName}.StyleConstants
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesFontsPackage.getName}.{FontStyle, FontWeight, UdashFonts}
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesUtilsPackage.getName}.{MediaQueries, StyleUtils}
          |
          |import scala.language.postfixOps
          |import scalacss.Attr
          |import scalacss.Defaults._
          |
          |object GlobalStyles extends StyleSheet.Inline {
          |  import dsl._
          |
          |  val reset = style(scalacss.ext.CssReset.meyer)
          |
          |  val global = style(
          |    unsafeRoot("#application") (
          |      height(100 %%)
          |    ),
          |
          |    unsafeRoot("html") (
          |      UdashFonts.acumin(),
          |      position.relative,
          |      height(100 %%),
          |      fontSize(62.5 %%),
          |      overflowY.scroll
          |    ),
          |
          |    unsafeRoot("body") (
          |      position.relative,
          |      height(100 %%),
          |      fontSize(1.6 rem)
          |    ),
          |
          |    unsafeRoot("p")(
          |      marginTop(1 rem),
          |      fontSize(1.6 rem),
          |      lineHeight(1.3),
          |
          |      &.firstChild (
          |        marginTop(`0`)
          |      )
          |    ),
          |
          |    unsafeRoot("li")(
          |      fontSize.inherit,
          |      lineHeight(1.3)
          |    ),
          |
          |    unsafeRoot("h1") (
          |      position.relative,
          |      UdashFonts.acumin(FontWeight.SemiBold),
          |      paddingTop(7 rem),
          |      paddingBottom(4.5 rem),
          |      lineHeight(1.2),
          |      fontSize(4.8 rem),
          |
          |      &.after (
          |        content := "\\"\\u2014\\"",
          |        position.absolute,
          |        left(`0`),
          |        bottom(`0`),
          |        fontSize(3.6 rem)
          |      ),
          |
          |      MediaQueries.phone(
          |        style(
          |          fontSize(3.2 rem)
          |        )
          |      )
          |    ),
          |
          |    unsafeRoot("h2") (
          |      UdashFonts.acumin(FontWeight.Regular),
          |      marginBottom(2 rem),
          |      lineHeight(1.2),
          |      fontSize(2.5 rem),
          |      wordWrap.breakWord,
          |
          |      MediaQueries.phone(
          |        style(
          |          fontSize(2.8 rem)
          |        )
          |      )
          |    ),
          |
          |    unsafeRoot("h3") (
          |      UdashFonts.acumin(FontWeight.ExtraLight),
          |      marginTop(4.5 rem),
          |      marginBottom(1.5 rem),
          |      lineHeight(1.2),
          |      fontSize(2.2 rem),
          |
          |      MediaQueries.phone(
          |        style(
          |          fontSize(2.6 rem)
          |        )
          |      )
          |    ),
          |
          |    unsafeRoot("h4") (
          |      UdashFonts.acumin(FontWeight.ExtraLight),
          |      marginTop(3.5 rem),
          |      marginBottom(1.5 rem),
          |      lineHeight(1.2),
          |      fontSize(2.4 rem),
          |
          |      MediaQueries.phone(
          |        style(
          |          fontSize(2 rem)
          |        )
          |      )
          |    ),
          |
          |    unsafeRoot("blockquote") (
          |      UdashFonts.acumin(FontWeight.ExtraLight, FontStyle.Italic),
          |      position.relative,
          |      margin(4 rem, `0`, 5 rem, 4.5 rem),
          |      padding(1.5 rem, 3 rem),
          |      fontSize(3.2 rem),
          |      color(StyleConstants.Colors.Grey),
          |
          |      &.before(
          |        StyleUtils.border(StyleConstants.Colors.Red, .3 rem),
          |        content := "\\" \\"",
          |        position.absolute,
          |        top(`0`),
          |        left(`0`),
          |        height(100 %%)
          |      ),
          |
          |      MediaQueries.phone(
          |        style(
          |          fontSize(2.4 rem)
          |        )
          |      )
          |    ),
          |
          |    unsafeRoot("a") (
          |      textDecoration := "none",
          |      outline(`0`).important,
          |
          |      &.link(
          |        textDecoration := "none"
          |      ),
          |
          |      &.hover (
          |        textDecoration := "none"
          |      ),
          |
          |      &.visited (
          |        color.inherit
          |      ),
          |
          |      &.hover (
          |        textDecoration := "underline"
          |      )
          |    ),
          |
          |    unsafeRoot("img")(
          |      maxWidth(100 %%),
          |      height.auto
          |    ),
          |
          |    unsafeRoot("svg") (
          |      display.block
          |    ),
          |
          |    unsafeRoot("object[type='image/svg+xml']") (
          |      display.block,
          |      pointerEvents := "none"
          |    ),
          |
          |    unsafeRoot("input") (
          |      padding(.5 rem, 1 rem),
          |      borderWidth(1 px),
          |      borderStyle.solid,
          |      borderColor(c"#cccccc"),
          |      borderRadius(.3 rem),
          |
          |      &.focus (
          |        outline.none
          |      )
          |    ),
          |
          |    unsafeRoot("input::-webkit-outer-spin-button")(
          |      Attr.real("-webkit-appearance") := "none",
          |      margin(`0`)
          |    ),
          |
          |    unsafeRoot("input::-webkit-inner-spin-button")(
          |      Attr.real("-webkit-appearance") := "none",
          |      margin(`0`)
          |    ),
          |
          |    unsafeRoot("textarea") (
          |      resize.none
          |    ),
          |
          |    unsafeRoot("strong")(
          |      fontWeight.bolder
          |    ),
          |
          |    unsafeRoot("b")(
          |      fontWeight.bold
          |    ),
          |
          |    unsafeRoot("i")(
          |      fontStyle.italic,
          |      fontWeight._600
          |    ),
          |
          |    unsafeRoot("*") (
          |      boxSizing.borderBox,
          |
          |      &.before (
          |        boxSizing.borderBox
          |      ),
          |
          |      &.after (
          |        boxSizing.borderBox
          |      )
          |    )
          |  )
          |
          |  val clearfix = style(
          |    &.before (
          |      content := "\\" \\"",
          |      display.table
          |    ),
          |
          |    &.after (
          |      content := "\\" \\"",
          |      display.table,
          |      clear.both
          |    )
          |  )
          |
          |  val col = style(
          |    position.relative,
          |    display.inlineBlock,
          |    verticalAlign.top
          |  )
          |
          |  val body = style(
          |    position.relative,
          |    width(StyleConstants.Sizes.BodyWidth px),
          |    height(100 %%),
          |    margin(0 px, auto),
          |
          |    MediaQueries.tabletLandscape(
          |      style(
          |        width(100 %%),
          |        paddingLeft(2 rem),
          |        paddingRight(2 rem)
          |      )
          |    ),
          |
          |    MediaQueries.phone(
          |      style(
          |        paddingLeft(3 %%),
          |        paddingRight(3 %%)
          |      )
          |    )
          |  )
          |
          |  val main = style(
          |    position.relative,
          |    minHeight :=! s"calc(100vh - $${StyleConstants.Sizes.HeaderHeight}px - $${StyleConstants.Sizes.FooterHeight}px)",
          |    paddingBottom(10 rem)
          |  )
          |
          |  val block = style(
          |    display.block
          |  )
          |
          |  val inline = style(
          |    display.inline
          |  )
          |
          |  val hidden = style(
          |    visibility.hidden
          |  )
          |
          |  val noMargin = style(
          |    margin(`0`).important
          |  )
          |
          |  val red = style(
          |    color(StyleConstants.Colors.Red).important
          |  )
          |
          |  val grey = style(
          |    color(StyleConstants.Colors.Grey).important
          |  )
          |}
          |""".stripMargin)

    writeFile(demoStylesScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}
          |
          |import java.util.concurrent.TimeUnit
          |
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesUtilsPackage.getName}.{MediaQueries, StyleUtils}
          |
          |import scala.concurrent.duration.FiniteDuration
          |import scala.language.postfixOps
          |import scalacss.Compose
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
          |  val navItem = style(
          |    position.relative,
          |    display.inlineBlock,
          |    verticalAlign.middle,
          |    paddingLeft(1.8 rem),
          |    paddingRight(1.8 rem),
          |
          |    &.firstChild (
          |      paddingLeft(0 px)
          |    ),
          |
          |    &.lastChild (
          |      paddingRight(0 px)
          |    ),
          |
          |    &.before.not(_.firstChild)(
          |      StyleUtils.absoluteMiddle,
          |      content := "\\"|\\"",
          |      left(`0`),
          |
          |      &.hover(
          |        textDecoration := "none"
          |      )
          |    )
          |  )
          |
          |  val underlineLink = style(
          |    position.relative,
          |    display.block,
          |    color.white,
          |
          |    &.after(
          |      StyleUtils.transition(transform, new FiniteDuration(250, TimeUnit.MILLISECONDS)),
          |      position.absolute,
          |      top(100 %%),
          |      left(`0`),
          |      content := "\\" \\"",
          |      width(100 %%),
          |      borderBottomColor.white,
          |      borderBottomWidth(1 px),
          |      borderBottomStyle.solid,
          |      transform := "scaleX(0)",
          |      transformOrigin := "100% 50%"
          |    ),
          |
          |    &.hover(
          |      color.white,
          |      cursor.pointer,
          |      textDecoration := "none",
          |
          |      &.after (
          |        transformOrigin := "0 50%",
          |        transform := "scaleX(1)"
          |      )
          |    )
          |  )
          |
          |  val underlineLinkBlack = style(
          |    underlineLink,
          |    display.inlineBlock,
          |    color.black,
          |
          |    &.after(
          |      borderBottomColor.black
          |    ),
          |
          |    &.hover (
          |      color.black
          |    )
          |  )(Compose.trust)
          |
          |  private val liBulletStyle = style(
          |    position.absolute,
          |    left(`0`),
          |    top(`0`)
          |  )
          |
          |  private val liStyle = style(
          |    position.relative,
          |    paddingLeft(2 rem),
          |    margin(.5 rem, `0`, .5 rem, 4.5 rem),
          |
          |    MediaQueries.phone(
          |      style(
          |        marginLeft(1.5 rem)
          |      )
          |    )
          |  )
          |
          |  val stepsList = style(
          |    counterReset := "steps",
          |    unsafeChild("li") (
          |      liStyle,
          |
          |      &.before(
          |        liBulletStyle,
          |        counterIncrement := "steps",
          |        content := "counters(steps, '.')\\".\\""
          |      )
          |    )
          |  )
          |}
         |""".stripMargin)

    writeFile(configScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${configPackage.getName}
          |object ExternalUrls {
          |  val udashGithub = "https://github.com/UdashFramework/"
          |
          |  val udashDemos = "https://github.com/UdashFramework/udash-demos"
          |
          |  val stackoverflow = "http://stackoverflow.com/questions/tagged/udash"
          |
          |  val avsystem = "http://www.avsystem.com/"
          |
          |  val homepage = "http://udash.io/"
          |}
          |""".stripMargin)

    writeFile(stylesConstantsScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesConstantsPackage.getName}
          |import scalacss.Defaults._
          |
          |object StyleConstants extends StyleSheet.Inline{
          |  import dsl._
          |
          |  /**
          |    * SIZES
          |    */
          |  object Sizes {
          |    val BodyWidth = 1075
          |
          |    val MinSiteHeight = 550
          |
          |    val HeaderHeight = 80
          |
          |    val HeaderHeightMobile = 80
          |
          |    val FooterHeight = 120
          |
          |    val MobileMenuButton = 50
          |  }
          |
          |  /**
          |    * COLORS
          |    */
          |  object Colors {
          |    val Red = c"#e30613"
          |
          |    val RedLight = c"#ff2727"
          |
          |    val GreyExtra = c"#ebebeb"
          |
          |    val GreySemi = c"#cfcfd6"
          |
          |    val Grey = c"#777785"
          |
          |    val Yellow = c"#ffd600"
          |
          |    val Cyan = c"#eef4f7"
          |  }
          |
          |  /**
          |    * MEDIA QUERIES
          |    */
          |  object MediaQueriesBounds {
          |    val TabletLandscapeMax = Sizes.BodyWidth - 1
          |
          |    val TabletLandscapeMin = 768
          |
          |    val TabletMax = TabletLandscapeMin - 1
          |
          |    val TabletMin = 481
          |
          |    val PhoneMax = TabletMin - 1
          |  }
          |}
          |""".stripMargin)

    writeFile(stylesFontsScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesFontsPackage.getName}
          |import scala.language.postfixOps
          |import scalacss.AV
          |import scalacss.Defaults._
          |
          |object UdashFonts extends StyleSheet.Inline {
          |  import dsl._
          |
          |  def acumin(fontWeight: AV = FontWeight.Regular, fontStyle: AV = FontStyle.Normal) = style(
          |    fontFamily :=! FontFamily.Acumin,
          |    fontStyle,
          |    fontWeight
          |  )
          |}
          |
          |object FontFamily {
          |  val Acumin = "'acumin-pro', san-serif"
          |}
          |
          |object FontWeight extends StyleSheet.Inline {
          |  import dsl._
          |  val ExtraLight: AV = fontWeight._200
          |  val Light: AV = fontWeight._300
          |  val Regular: AV  = fontWeight._400
          |  val SemiBold: AV = fontWeight._600
          |  val Bold: AV = fontWeight._700
          |}
          |
          |object FontStyle extends StyleSheet.Inline {
          |  import dsl._
          |  val Normal: AV = fontStyle.normal
          |  val Italic: AV = fontStyle.italic
          |}
          |""".stripMargin)

    writeFile(stylesHeaderScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesPartialsPackage.getName}
          |import java.util.concurrent.TimeUnit
          |
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesConstantsPackage.getName}.StyleConstants
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesUtilsPackage.getName}.{MediaQueries, StyleUtils}
          |
          |import scala.concurrent.duration.FiniteDuration
          |import scala.language.postfixOps
          |import scalacss.Defaults._
          |
          |object HeaderStyles extends StyleSheet.Inline {
          |  import dsl._
          |
          |  val header = style(
          |    position.relative,
          |    backgroundColor.black,
          |    height(StyleConstants.Sizes.HeaderHeight px),
          |    fontSize(1.6 rem),
          |    zIndex(99),
          |
          |    MediaQueries.tabletPortrait(
          |      style(
          |        height(StyleConstants.Sizes.HeaderHeight * .7 px)
          |      )
          |    )
          |  )
          |
          |  val headerLeft = style(
          |    position.relative,
          |    float.left,
          |    height(100 %%)
          |  )
          |
          |  val headerRight = style(
          |    position.relative,
          |    float.right,
          |    height(100 %%)
          |  )
          |
          |  val headerLogo = style(
          |    StyleUtils.relativeMiddle,
          |    display.inlineBlock,
          |    verticalAlign.top,
          |    width(130 px),
          |    marginRight(25 px),
          |
          |    MediaQueries.tabletPortrait(
          |      style(
          |        width(130 * .8 px)
          |      )
          |    )
          |  )
          |
          |  val headerNav = style(
          |    StyleUtils.relativeMiddle,
          |    display.inlineBlock,
          |    verticalAlign.top,
          |    color.white
          |  )
          |
          |  val headerSocial = style(
          |    StyleUtils.relativeMiddle
          |  )
          |
          |  val headerSocialItem = style(
          |    display.inlineBlock,
          |    marginLeft(2 rem)
          |  )
          |
          |  private val socialLink = style(
          |    position.relative,
          |    display.block,
          |    width(33 px),
          |
          |    unsafeChild("svg") (
          |      StyleUtils.transition()
          |    ),
          |
          |    MediaQueries.tabletPortrait(
          |      style(
          |        width(25 px)
          |      )
          |    )
          |  )
          |
          |  val headerSocialLink = style(
          |    socialLink,
          |
          |    unsafeChild("svg") (
          |      svgFill := c"#fff"
          |    ),
          |
          |    &.hover (
          |      unsafeChild("svg") (
          |        svgFill := StyleConstants.Colors.Red
          |      )
          |    )
          |  )
          |
          |  val headerSocialLinkYellow = style(
          |    socialLink,
          |
          |    unsafeChild("svg") (
          |      svgFill := StyleConstants.Colors.Yellow
          |    ),
          |
          |    &.hover (
          |      unsafeChild(s".$${tooltip.htmlClass}")(
          |        visibility.visible,
          |        opacity(1)
          |      ),
          |
          |      unsafeChild(s".$${tooltipTop.htmlClass}")(
          |        transitionDelay(new FiniteDuration(0, TimeUnit.MILLISECONDS)),
          |        transform := "scaleX(1)"
          |      ),
          |
          |      unsafeChild(s".$${tooltipTextInner.htmlClass}")(
          |        transitionDelay(new FiniteDuration(350, TimeUnit.MILLISECONDS)),
          |        transform := "translateY(0)"
          |      )
          |    )
          |  )
          |
          |  lazy val tooltip = style(
          |    StyleUtils.transition(new FiniteDuration(150, TimeUnit.MILLISECONDS)),
          |    position.absolute,
          |    top :=! "calc(100% + 10px)",
          |    right(`0`),
          |    fontSize(1.2 rem),
          |    color.black,
          |    textAlign.center,
          |    visibility.hidden,
          |    opacity(0),
          |    pointerEvents := "none",
          |
          |    MediaQueries.tabletLandscape(
          |      style(
          |        display.none
          |      )
          |    )
          |  )
          |
          |  lazy val tooltipTop = style(
          |    StyleUtils.transition(new FiniteDuration(350, TimeUnit.MILLISECONDS)),
          |    transitionDelay(new FiniteDuration(200, TimeUnit.MILLISECONDS)),
          |    position.relative,
          |    width(100 %%),
          |    backgroundColor(StyleConstants.Colors.Red),
          |    height(4 px),
          |    transformOrigin := "calc(100% - 9px) 0",
          |    transform := "scaleX(.2)",
          |    zIndex(9),
          |
          |    &.after(
          |      content := "\\" \\"",
          |      position.absolute,
          |      bottom :=! "calc(100% - 1px)",
          |      right(9 px),
          |      marginLeft(-6 px),
          |      width(`0`),
          |      height(`0`),
          |      borderBottomWidth(6 px),
          |      borderBottomStyle.solid,
          |      borderBottomColor(StyleConstants.Colors.Red),
          |      borderRightWidth(6 px),
          |      borderRightStyle.solid,
          |      borderRightColor.transparent,
          |      borderLeftWidth(6 px),
          |      borderLeftStyle.solid,
          |      borderLeftColor.transparent
          |    )
          |  )
          |
          |  val tooltipText = style(
          |    position.relative,
          |    width(100 %%),
          |    overflow.hidden
          |  )
          |
          |  lazy val tooltipTextInner = style(
          |    StyleUtils.transition(new FiniteDuration(200, TimeUnit.MILLISECONDS)),
          |    position.relative,
          |    width(100 %%),
          |    padding(10 px, 15 px),
          |    color.white,
          |    backgroundColor(StyleConstants.Colors.RedLight),
          |    whiteSpace.nowrap,
          |    transform := "translateY(-100%)"
          |  )
          |}
          |""".stripMargin)


    writeFile(stylesFooterScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesPartialsPackage.getName}
          |
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesConstantsPackage.getName}.StyleConstants
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesFontsPackage.getName}.{FontWeight, UdashFonts}
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesUtilsPackage.getName}.{MediaQueries, StyleUtils}
          |
          |import scala.language.postfixOps
          |import scalacss.Defaults._
          |
          |object FooterStyles extends StyleSheet.Inline {
          |  import dsl._
          |
          |  val footer = style(
          |    backgroundColor.black,
          |    height(StyleConstants.Sizes.FooterHeight px),
          |    fontSize(1.2 rem),
          |    color.white,
          |
          |    MediaQueries.phone(
          |      style(
          |        height.auto,
          |        padding(2 rem, `0`)
          |      )
          |    )
          |  )
          |
          |  val footerInner = style(
          |    StyleUtils.relativeMiddle,
          |
          |    MediaQueries.phone(
          |      style(
          |        top.auto,
          |        transform := "none"
          |      )
          |    )
          |  )
          |
          |  val footerLogo = style(
          |    display.inlineBlock,
          |    verticalAlign.middle,
          |    width(50 px),
          |    marginRight(25 px)
          |  )
          |
          |  val footerLinks = style(
          |    display.inlineBlock,
          |    verticalAlign.middle
          |  )
          |
          |  val footerMore = style(
          |    UdashFonts.acumin(FontWeight.SemiBold),
          |    marginBottom(1.5 rem),
          |    fontSize(2.2 rem)
          |  )
          |
          |  val footerCopyrights = style(
          |    position.absolute,
          |    right(`0`),
          |    bottom(`0`),
          |    fontSize.inherit,
          |
          |    MediaQueries.tabletPortrait(
          |      style(
          |        position.relative,
          |        textAlign.right
          |      )
          |    )
          |  )
          |
          |  val footerAvsystemLink = style(
          |    StyleUtils.transition(),
          |    color.inherit,
          |    textDecoration := "underline",
          |
          |    &.hover (
          |      color(StyleConstants.Colors.Yellow)
          |    ),
          |
          |    &.visited (
          |      color.inherit,
          |
          |      &.hover (
          |        color(StyleConstants.Colors.Yellow)
          |      )
          |    )
          |  )
          |}
          |""".stripMargin)

    writeFile(stylesMediaQueriesScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesUtilsPackage.getName}
          |
          |
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesConstantsPackage.getName}.StyleConstants
          |
          |import scalacss.Defaults._
          |import scala.language.postfixOps
          |
          |object MediaQueries extends StyleSheet.Inline {
          |  import dsl._
          |
          |  def tabletLandscape(properties: StyleA) = style(
          |    media.screen.minWidth(1 px).maxWidth(StyleConstants.MediaQueriesBounds.TabletLandscapeMax px) (
          |      properties
          |    )
          |  )
          |
          |  def tabletPortrait(properties: StyleA) = style(
          |    media.screen.minWidth(1 px).maxWidth(StyleConstants.MediaQueriesBounds.TabletMax px) (
          |      properties
          |    )
          |  )
          |
          |  def phone(properties: StyleA) = style(
          |    media.screen.minWidth(1 px).maxWidth(StyleConstants.MediaQueriesBounds.PhoneMax px) (
          |      properties
          |    )
          |  )
          |}
          |""".stripMargin)

    writeFile(stylesStyleUtilsScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesUtilsPackage.getName}
          |import java.util.concurrent.TimeUnit
          |
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesConstantsPackage.getName}.StyleConstants
          |
          |import scala.concurrent.duration.FiniteDuration
          |import scala.language.postfixOps
          |import scalacss.{AV, Attr, Length, ValueT}
          |import scalacss.Defaults._
          |
          |object StyleUtils extends StyleSheet.Inline {
          |  import dsl._
          |
          |  val middle = style(
          |    top(50 %%),
          |    transform := "translateY(-50%)"
          |  )
          |
          |  val center = style(
          |    top(50 %%),
          |    left(50 %%),
          |    transform := "translateY(-50%) translateX(-50%)"
          |  )
          |
          |  val relativeMiddle = style(
          |    middle,
          |    position.relative
          |  )
          |
          |  val absoluteMiddle = style(
          |    middle,
          |    position.absolute
          |  )
          |
          |  val absoluteCenter = style(
          |    center,
          |    position.absolute
          |  )
          |
          |  def transition(): StyleA = style(
          |    transitionProperty := "all",
          |    transitionDuration(new FiniteDuration(250, TimeUnit.MILLISECONDS)),
          |    transitionTimingFunction.easeInOut
          |  )
          |
          |  def transition(duration: FiniteDuration): StyleA = style(
          |    transitionProperty := "all",
          |    transitionDuration(duration),
          |    transitionTimingFunction.easeInOut
          |  )
          |
          |  def transition(property: Attr, duration: FiniteDuration): StyleA = style(
          |    transitionProperty := property.toString(),
          |    transitionDuration(duration),
          |    transitionTimingFunction.easeInOut
          |  )
          |
          |  def border(bColor: ValueT[ValueT.Color] = StyleConstants.Colors.GreyExtra, bWidth: Length[Double] = 1.0 px, bStyle: AV = borderStyle.solid): StyleA = style(
          |    borderWidth(bWidth),
          |    bStyle,
          |    borderColor(bColor)
          |  )
          |
          |  def bShadow(x: Int = 2, y: Int = 2, blur: Int = 5, spread: Int = 0, color: ValueT[ValueT.Color] = c"#000000", opacity: Double = .4, inset: Boolean = false): StyleA = style(
          |    boxShadow := s"$${if (inset) "inset " else ""}$${x}px $${y}px $${blur}px $${spread}px $${hexToRGBA(color, opacity)}"
          |  )
          |
          |  private def hexToRGBA(color: ValueT[ValueT.Color], opacity: Double = 1): String = {
          |    val cNumber = Integer.parseInt(color.value.replace("#", ""), 16)
          |    val r = (cNumber.toInt >> 16) & 0xFF
          |    val g = (cNumber.toInt >>  8) & 0xFF
          |    val b = (cNumber.toInt >>  0) & 0xFF
          |
          |    s"rgba($$r, $$g, $$b, $$opacity)"
          |  }
          |}
          |""".stripMargin)

    writeFile(footerScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.${componentsPackage.getName}
          |import ${settings.rootPackage.mkPackage()}.${configPackage.getName}.ExternalUrls
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.{DemoStyles, GlobalStyles}
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesPartialsPackage.getName}.FooterStyles
          |import org.scalajs.dom.raw.Element
          |
          |import scalatags.JsDom.all._
          |import scalacss.ScalatagsCss._
          |
          |object Footer {
          |  private lazy val template = footer(FooterStyles.footer)(
          |    div(GlobalStyles.body)(
          |      div(FooterStyles.footerInner)(
          |        a(FooterStyles.footerLogo, href := ExternalUrls.homepage)(
          |          Image("udash_logo.png", "Udash Framework", GlobalStyles.block)
          |        ),
          |        div(FooterStyles.footerLinks)(
          |          p(FooterStyles.footerMore)("See more"),
          |          ul(
          |            li(DemoStyles.navItem)(
          |              a(href := ExternalUrls.udashDemos, target := "_blank", DemoStyles.underlineLink)("Github demo")
          |            ),
          |            li(DemoStyles.navItem)(
          |              a(href := ExternalUrls.stackoverflow, target := "_blank", DemoStyles.underlineLink)("StackOverflow questions")
          |            )
          |          )
          |        ),
          |        p(FooterStyles.footerCopyrights)("Proudly made by ", a(FooterStyles.footerAvsystemLink, href := ExternalUrls.avsystem, target := "_blank")("AVSystem"))
          |      )
          |    )
          |  ).render
          |
          |  def getTemplate: Element = template
          |}
          |""".stripMargin)

    writeFile(headerScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.${componentsPackage.getName}
          |import ${settings.rootPackage.mkPackage()}.IndexState
          |import ${settings.rootPackage.mkPackage()}.${configPackage.getName}.ExternalUrls
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.GlobalStyles
          |import ${settings.rootPackage.mkPackage()}.${settings.stylesSubPackage.mkPackage()}.${stylesPartialsPackage.getName}.HeaderStyles
          |import org.scalajs.dom.raw.Element
          |
          |import scalatags.JsDom.all._
          |import scalacss.ScalatagsCss._
          |import ${settings.rootPackage.mkPackage()}.Context._
          |
          |object Header {
          |  private lazy val template = header(HeaderStyles.header)(
          |    div(GlobalStyles.body, GlobalStyles.clearfix)(
          |      div(HeaderStyles.headerLeft)(
          |        a(HeaderStyles.headerLogo, href := IndexState.url)(
          |          Image("udash_logo_m.png", "Udash Framework", GlobalStyles.block)
          |        )
          |      ),
          |      div(HeaderStyles.headerRight)(
          |        ul(HeaderStyles.headerSocial)(
          |          li(HeaderStyles.headerSocialItem)(
          |            a(href := ExternalUrls.udashGithub, HeaderStyles.headerSocialLink, target := "_blank")(
          |              Image("icon_github.png", "Github")
          |            )
          |          ),
          |          li(HeaderStyles.headerSocialItem)(
          |            a(href := ExternalUrls.stackoverflow, HeaderStyles.headerSocialLink, target := "_blank")(
          |              Image("icon_stackoverflow.png", "StackOverflow")
          |            )
          |          ),
          |          li(HeaderStyles.headerSocialItem)(
          |            a(href := ExternalUrls.avsystem, HeaderStyles.headerSocialLinkYellow, target := "_blank")(
          |              Image("icon_avsystem.png", "Proudly made by AVSystem"),
          |              div(HeaderStyles.tooltip)(
          |                div(HeaderStyles.tooltipTop),
          |                div(HeaderStyles.tooltipText)(
          |                  div(HeaderStyles.tooltipTextInner)(
          |                    "Proudly made by AVSystem"
          |                  )
          |                )
          |              )
          |            )
          |          )
          |        )
          |      )
          |    )
          |  ).render
          |
          |  def getTemplate: Element = template
          |}
          |""".stripMargin)

    writeFile(imageFactoryScala)(
      s"""package ${settings.rootPackage.mkPackage()}.${settings.viewsSubPackage.mkPackage()}.${componentsPackage.getName}
          |import org.scalajs.dom
          |import scalatags.JsDom
          |
          |class ImageFactory(prefix: String) {
          |  import scalatags.JsDom.all._
          |  def apply(name: String, altText: String, xs: Modifier*): JsDom.TypedTag[dom.html.Image] = {
          |    img(src := s"$$prefix/$$name", alt := altText, xs)
          |  }
          |}
          |
          |object Image extends ImageFactory("assets/images")
          |""".stripMargin)
  }
}
