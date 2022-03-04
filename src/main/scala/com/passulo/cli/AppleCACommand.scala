package com.passulo.cli

import com.passulo.util.Http
import com.passulo.{Config, StdOutText}
import picocli.CommandLine.{Command, Option}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.io.File
import java.util.concurrent.Callable

@Command(
  name = "appleCA",
  mixinStandardHelpOptions = true,
  description = Array("Downloads the Apple WWDR CA G4 CertificateApple WWDR CA G4 Certificate"),
  usageHelpWidth = 120
)
class AppleCACommand extends Callable[Int] {

  @Option(names = Array("-n", "--name"), description = Array("Overrides the name for the file. Default is `AppleWWDRCAG4.cer`"))
  var name: String = _

  def nameFromConfig: String =
    ConfigSource.file("passulo.conf").load[Config] match {
      case Left(_) =>
        println("passulo.conf not found, using fallback name")
        "AppleWWDRCAG4.cer"
      case Right(value) => value.keys.appleCaCert
    }

  def call(): Int = {

    val filename = if (name == null || name.isBlank) nameFromConfig else name
    val fileRef  = new File(filename)
    val response = Http.download("https://www.apple.com/certificateauthority/AppleWWDRCAG4.cer", fileRef.toPath)
    response.statusCode() match {
      case 200  => println(StdOutText.success(s"Successfully loaded certificate to ${fileRef.getAbsolutePath}"))
      case code => println(StdOutText.error(s"Error from Server (Code $code"))
    }
    0
  }
}
