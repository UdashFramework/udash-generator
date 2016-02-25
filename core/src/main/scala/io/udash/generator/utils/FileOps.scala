package io.udash.generator.utils

import java.io._

import io.udash.generator.plugins.Placeholder

import scala.io.Source
import scala.util.matching.Regex

/** Basic file operations */
trait FileOps {
  /** Writes `content` into `file`. */
  protected def writeFile(file: File)(content: String) = {
    new PrintWriter(file) {
      write(content)
      close()
    }
  }

  /** Appends `content` into `file`. */
  protected def appendFile(file: File)(content: String) = {
    new PrintWriter(new BufferedWriter(new FileWriter(file.getAbsolutePath, true))) {
      write(content)
      close()
    }
  }

  /** Replaces all parts of `file` matching `regex` with `replacement`. */
  protected def replaceInFile(file: File)(regex: String, replacement: String) = {
    val current: String = readWholeFile(file)
    new PrintWriter(file) {
      write(current.replaceAll(regex, replacement))
      close()
    }
  }

  /** Adds `content` before all occurrences of `placeholder` in `file`. */
  protected def appendOnPlaceholder(file: File)(placeholder: Placeholder, content: String) =
    replaceInFile(file)(Regex.quote(placeholder.toString), content+placeholder.toString)

  /** Removes all parts of `file` matching `regex`. */
  protected def removeFromFile(file: File)(regex: String) =
    replaceInFile(file)(regex, "")

  protected def removeFileOrDir(file: File): Unit = {
    if (file.exists()) {
      if (file.isDirectory) {
        file.listFiles().foreach(removeFileOrDir)
      }
      file.delete()
    }
  }

  private def readWholeFile(file: File): String =
    Source.fromFile(file).getLines.mkString("\n")
}
