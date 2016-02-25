package io.udash.generator

import java.io.File

import io.udash.generator.exceptions._

package object utils {
  def createFiles(files: Seq[File], requireNotExists: Boolean = false): Unit = {
    files.foreach((file: File) => {
      if (!file.createNewFile() && requireNotExists) throw FileCreationError(s"${file.getAbsolutePath} already exists!")
    })
  }

  def createDirs(files: Seq[File], requireNotExists: Boolean = false): Unit = {
    files.foreach((file: File) => {
      if (!file.mkdirs() && requireNotExists) throw FileCreationError(s"${file.getAbsolutePath} already exists!")
    })
  }

  def requireFilesExist(files: Seq[File]): Unit = {
    files.foreach((file: File) => {
      if (!file.exists()) throw FileDoesNotExist(s"${file.getAbsolutePath} does not exist!")
    })
  }

  implicit class SeqOps(private val s: Seq[String]) extends AnyVal {
    def mkPackage(): String = s.mkString(".")
  }

  implicit class FileExt(private val file: File) extends AnyVal {
    def subFile(f: String): File = new File(s"${file.getAbsolutePath}${File.separator}$f")
  }
}
