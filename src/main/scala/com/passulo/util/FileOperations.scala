package com.passulo.util

import com.passulo.StdOutText

import java.io.{File, FileNotFoundException}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.{BufferedSource, Source}
import scala.util.{Success, Try}

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

  def loadFile(filename: String): BufferedSource =
    Try(Source.fromFile(filename)).orElse(Try(Source.fromResource(filename))).orElse(Try(Source.fromURL(filename))).getOrElse {
      StdOutText.error(s"File not found.")
      throw new FileNotFoundException(filename)
    }

  def loadFile(file: File): BufferedSource =
    Source.fromFile(file)
}
