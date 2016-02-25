package io.udash.generator

package object exceptions {
  case class FileCreationError(msg: String) extends RuntimeException(msg)
  case class FileDoesNotExist(msg: String) extends RuntimeException(msg)

  case class InvalidConfiguration(msg: String) extends RuntimeException(msg)

  case class InvalidConfigDecisionResponse(msg: String) extends RuntimeException(msg)
}