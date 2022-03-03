package com.passulo.util

import com.passulo.cli.FileNotFound

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.util.Success

object FileOperations {
  def writeFile(filename: String, text: String, overwrite: Boolean): Either[String, Success.type] = {
    val fileExists = Paths.get(filename).toFile.exists()
    if (fileExists && !overwrite) {
      Left(s"File $filename already exists, and you didn't specify to overwrite it. Stopping.")
    } else if (fileExists && overwrite) {
      println(s"File $filename already exists, overwriting…")
      val options = Seq(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.DSYNC, StandardOpenOption.TRUNCATE_EXISTING)
      Files.write(Paths.get(filename), text.getBytes(StandardCharsets.UTF_8), options*)
      Right(Success)
    } else {
      val options = Seq(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.DSYNC)
      Files.write(Paths.get(filename), text.getBytes(StandardCharsets.UTF_8), options*)
      Right(Success)
    }
  }

  def loadFile(filename: String): Either[FileNotFound, File] = {
    println(s"Trying to load file $filename")
    val file = new File(filename)
    if (file.exists() && file.canRead)
      Right(file)
    else {
      Left(FileNotFound(s"File at ${file.getAbsolutePath} could not be found!"))
    }
  }
}
