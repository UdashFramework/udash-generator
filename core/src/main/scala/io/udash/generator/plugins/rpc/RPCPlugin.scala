package io.udash.generator.plugins.rpc

import java.io.File

import io.udash.generator.exceptions.InvalidConfiguration
import io.udash.generator.plugins._
import io.udash.generator.plugins.jetty.JettyLauncherPlugin
import io.udash.generator.plugins.sbt.SBTProjectFiles
import io.udash.generator.plugins.utils.{FrontendPaths, UtilPaths}
import io.udash.generator.utils._
import io.udash.generator.{FrontendOnlyProject, GeneratorPlugin, GeneratorSettings, StandardProject}

object RPCPlugin extends GeneratorPlugin with SBTProjectFiles with FrontendPaths with UtilPaths {

  override val dependencies = Seq(JettyLauncherPlugin)

  val rpcDir = "rpc"

  override def run(settings: GeneratorSettings): GeneratorSettings = {
    settings.projectType match {
      case FrontendOnlyProject =>
        throw InvalidConfiguration("You can not add RPC into frontend only project.")
      case StandardProject(backend, shared, frontend) =>
        updateSBTConfig(settings)
        createRPCInterfaces(rootPackageInSrc(settings.rootDirectory.subFile(shared), settings), settings)
        createBackendImplementation(rootPackageInSrc(settings.rootDirectory.subFile(backend), settings), settings)
        createFrontendImplementation(rootPackageInSrc(settings.rootDirectory.subFile(frontend), settings), settings)
    }

    settings
  }

  private def updateSBTConfig(settings: GeneratorSettings): Unit = {
    val sbtDepsFile = dependenciesScala(settings)

    requireFilesExist(Seq(sbtDepsFile))

    appendOnPlaceholder(sbtDepsFile)(DependenciesFrontendPlaceholder,
      s""",
         |    "io.udash" %%% "udash-rpc-frontend" % udashVersion""".stripMargin)

    appendOnPlaceholder(sbtDepsFile)(DependenciesCrossPlaceholder,
      s""",
         |    "io.udash" %%% "udash-rpc-shared" % udashVersion""".stripMargin)

    appendOnPlaceholder(sbtDepsFile)(DependenciesBackendPlaceholder,
      s""",
         |    "io.udash" %% "udash-rpc-backend" % udashVersion,
         |    "org.eclipse.jetty.websocket" % "websocket-server" % jettyVersion""".stripMargin)
  }

  private def createRPCInterfaces(rootPackage: File, settings: GeneratorSettings): Unit = {
    val rpcPackage = rootPackage.subFile(rpcDir)
    val clientRPCScala = rpcPackage.subFile("MainClientRPC.scala")
    val serverRPCScala = rpcPackage.subFile("MainServerRPC.scala")

    requireFilesExist(Seq(rootPackage))
    createDirs(Seq(rpcPackage))
    createFiles(Seq(clientRPCScala, serverRPCScala))

    writeFile(clientRPCScala)(
      s"""package ${settings.rootPackage.mkPackage()}.$rpcDir
         |
         |import com.avsystem.commons.rpc.RPC
         |import io.udash.rpc._
         |
         |@RPC
         |trait MainClientRPC {
         |  def push(number: Int): Unit
         |}
       """.stripMargin
    )

    writeFile(serverRPCScala)(
      s"""package ${settings.rootPackage.mkPackage()}.$rpcDir
         |
         |import com.avsystem.commons.rpc.RPC
         |import io.udash.rpc._
         |import scala.concurrent.Future
         |
         |@RPC
         |trait MainServerRPC {
         |  def hello(name: String): Future[String]
         |  def pushMe(): Unit
         |}
         |
       """.stripMargin
    )
  }

  private def createBackendImplementation(rootPackage: File, settings: GeneratorSettings): Unit = {
    val jettyDir = "jetty"
    val jettyPackage = rootPackage.subFile(jettyDir)
    val rpcPackage = rootPackage.subFile(rpcDir)
    val appServerScala = jettyPackage.subFile("ApplicationServer.scala")
    val exposedRpcInterfacesScala = rpcPackage.subFile("ExposedRpcInterfaces.scala")
    val clientRPCScala = rpcPackage.subFile("ClientRPC.scala")

    requireFilesExist(Seq(rootPackage, jettyPackage, appServerScala))
    createDirs(Seq(rpcPackage))
    createFiles(Seq(clientRPCScala, exposedRpcInterfacesScala))

    writeFile(clientRPCScala)(
      s"""package ${settings.rootPackage.mkPackage()}.$rpcDir
         |
         |import io.udash.rpc._
         |
         |import scala.concurrent.ExecutionContext
         |
         |object ClientRPC {
         |  def apply(target: ClientRPCTarget)(implicit ec: ExecutionContext): MainClientRPC = {
         |    new DefaultClientRPC[MainClientRPC](target).get
         |  }
         |}
         |
       """.stripMargin
    )

    writeFile(exposedRpcInterfacesScala)(
      s"""package ${settings.rootPackage.mkPackage()}.$rpcDir
         |
         |import io.udash.rpc._
         |
         |import scala.concurrent.Future
         |import scala.concurrent.ExecutionContext.Implicits.global
         |
         |class ExposedRpcInterfaces(implicit clientId: ClientId) extends MainServerRPC {
         |  override def hello(name: String): Future[String] =
         |    Future.successful(s"Hello, ${"$name"}!")
         |
         |  override def pushMe(): Unit =
         |    ClientRPC(clientId).push(42)
         |}
         |
       """.stripMargin
    )

    appendOnPlaceholder(appServerScala)(BackendAppServerPlaceholder,
      s"""
         |
         |  private val atmosphereHolder = {
         |    import io.udash.rpc._
         |    import ${settings.rootPackage.mkPackage()}.$rpcDir._
         |    import scala.concurrent.ExecutionContext.Implicits.global
         |
         |    val config = new DefaultAtmosphereServiceConfig[MainServerRPC]((clientId) => new DefaultExposesServerRPC[MainServerRPC](new ExposedRpcInterfaces()(clientId)))
         |    val framework = new DefaultAtmosphereFramework(config)
         |
         |    //Disabling all files scan during service auto-configuration,
         |    //as it's quite time-consuming - a few seconds long.
         |    //
         |    //If it's really required, enable it, but at the cost of start-up overhead or some tuning has to be made.
         |    //For that purpose, check what is going on in:
         |    //- DefaultAnnotationProcessor
         |    //- org.atmosphere.cpr.AtmosphereFramework.autoConfigureService
         |    framework.allowAllClassesScan(false)
         |
         |    framework.init()
         |
         |    val atmosphereHolder = new ServletHolder(new RpcServlet(framework))
         |    atmosphereHolder.setAsyncSupported(true)
         |    atmosphereHolder
         |  }
         |  contextHandler.addServlet(atmosphereHolder, "/atm/*")
       """.stripMargin
    )
  }

  private def createFrontendImplementation(rootPackage: File, settings: GeneratorSettings): Unit = {
    val rpcPackage = rootPackage.subFile(rpcDir)
    val rpcServiceScala = rpcPackage.subFile("RPCService.scala")
    val initScala = rootPackage.subFile("init.scala")

    requireFilesExist(Seq(rootPackage, initScala))
    createDirs(Seq(rpcPackage))
    createFiles(Seq(rpcServiceScala))

    writeFile(rpcServiceScala)(
      s"""package ${settings.rootPackage.mkPackage()}.$rpcDir
         |
         |class RPCService extends MainClientRPC {
         |  override def push(number: Int): Unit =
         |    println(s"Push from server: ${"$number"}")
         |}
         |
       """.stripMargin
    )

    appendOnPlaceholder(initScala)(FrontendContextPlaceholder,
      s"""
         |
         |  import io.udash.rpc._
         |  import ${settings.rootPackage.mkPackage()}.$rpcDir._
         |  val serverRpc = DefaultServerRPC[MainClientRPC, MainServerRPC](new RPCService)
       """.stripMargin
    )
  }
}
