package io.udash.generator.configuration

import java.io.File

import io.udash.generator.{FrontendOnlyProject, ProjectType, StandardProject}

sealed abstract class Decision[ResponseType](val default: ResponseType) {
  def response: Option[ResponseType]
  def validator(): Option[String] = None
}

sealed abstract class SelectDecision[ResponseType](default: ResponseType) extends Decision[ResponseType](default) {
  def options: Seq[ResponseType]
  override def validator(): Option[String] =
    if (!options.contains(response.get)) Some(s"Select one of following values: $options") else None
}

sealed abstract class NonEmptyStringDecision(errorMsg: String, default: String) extends Decision[String](default) {
  override def validator(): Option[String] =
    if (response.get.isEmpty || response.get.trim.isEmpty) Some(errorMsg) else None
}

/** Project will be generated in this directory. */
case class RootDirectory(override val response: Option[File] = None) extends Decision[File](new File("udash-app")) {
  override def validator(): Option[String] = {
    val target = response.get
    if (target.exists() && !target.isDirectory) Some(s"${target.getAbsolutePath} is not a directory.")
    else if (target.exists() && !target.canWrite) Some(s"You can not write in ${target.getAbsolutePath}.")
    else None
  }
}

/** If `true`, root project directory will be cleared before generation. */
case class ClearRootDirectory(override val response: Option[Boolean] = None) extends Decision[Boolean](false)

/** Generated project name. */
case class ProjectName(override val response: Option[String] = None) extends NonEmptyStringDecision("Project name cannot be empty!", "udash-app") {
  override def validator(): Option[String] =
    super.validator() match {
      case None =>
        if ("[^A-Za-z\\d-]".r.findFirstIn(response.get).nonEmpty) Some("Project name can contain only characters, digits and '-' sign.")
        else None
      case error => error
    }
}

/** Organization name. */
case class Organization(override val response: Option[String] = None) extends NonEmptyStringDecision("Organization name cannot be empty!", "com.example") {
  override def validator(): Option[String] =
    super.validator() match {
      case None =>
        if ("^[A-Za-z]+(\\.[A-Za-z][A-Za-z\\d-]*)*$".r.findFirstIn(response.get).isEmpty)
          Some(
            """Organization name should be provided in reverse domain notation, eg. 'io.udash'.
              |The first part can contain only characters, the others can contains characters, digits and '-' sign.
            """.stripMargin)
        else None
      case error => error
    }
}

/** Root source code package. */
case class RootPackage(override val response: Option[Seq[String]] = None) extends Decision[Seq[String]](Seq("com", "example"))  {
  override def validator(): Option[String] =
    if (response.isEmpty || response.get.isEmpty)
      Some("Root package can not be empty.")
    else if (response.get.exists(part => !part.matches("^[A-Za-z][A-Za-z\\d]*$")))
      Some(
        """Root package should be provided in reverse domain notation, eg. 'io.udash.app'.
          |The first part can contain only characters, the others can contains characters and digits.
        """.stripMargin)
    else None
}

/** Frontend-only or standard Udash project configuration. */
case class ProjectTypeSelect(override val response: Option[ProjectType] = None) extends SelectDecision[ProjectType](StandardProject("backend", "shared", "frontend")) {
  override val options: Seq[ProjectType] = Seq(FrontendOnlyProject, StandardProject("backend", "shared", "frontend"))
}

/** Names of modules in standard Udash project configuration. */
case class StdProjectTypeModulesSelect(override val response: Option[StandardProject] = None) extends Decision[StandardProject](StandardProject("backend", "shared", "frontend")) {
  override def validator(): Option[String] = {
    val project = response.get
    if (project.backend == project.frontend || project.backend == project.shared || project.frontend == project.shared)
      Some("Module names must be unique.")
    else if (project.backend.trim.isEmpty || project.shared.trim.isEmpty || project.frontend.trim.isEmpty)
      Some("Module names must not be empty.")
    else if (Seq(project.backend, project.frontend, project.shared).exists(name => "[^\\w\\d-]".r.findFirstIn(name).nonEmpty))
      Some("Module name can contain only characters, digits and '-' sign.")
    else None
  }
}

/* If `true`, generator creates basic frontend application code. */
case class CreateBasicFrontendApp(override val response: Option[Boolean] = None) extends Decision[Boolean](true)

/* If `true`, generator creates frontend demo views. */
case class CreateFrontendDemos(override val response: Option[Boolean] = None) extends Decision[Boolean](true)

/* If `true`, generator creates ScalaCSS demo views. */
case class CreateScalaCSSDemos(override val response: Option[Boolean] = None) extends Decision[Boolean](true)

/* If `true`, generator creates Jetty server serving frontend application. */
case class CreateJettyLauncher(override val response: Option[Boolean] = None) extends Decision[Boolean](true)

/* If `true`, generator creates example RPC interfaces and implementation in `frontend` and `backend` modules. */
case class CreateRPC(override val response: Option[Boolean] = None) extends Decision[Boolean](true)

/* If `true`, generator creates RPC demo views. */
case class CreateRPCDemos(override val response: Option[Boolean] = None) extends Decision[Boolean](true)

/* If `true`, generator starts project generation process. */
case class RunGenerator(override val response: Option[Boolean] = None) extends Decision[Boolean](true)

/* If `true`, generator enables the app to use the JsWorkbench SBT Plugin during development. */
case class EnableJsWorkbench(override val response: Option[Boolean] = None) extends Decision[Boolean](true)