package com.passulo.cli

import com.passulo.util.CryptoHelper
import com.passulo.{Config, StdOutText}
import picocli.CommandLine.{Command, Option}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.awt.Desktop
import java.io.File
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable

@Command(
  name = "register",
  mixinStandardHelpOptions = true,
  description = Array("Registers the public key with a Passulo-Server.")
)
class RegisterCommand extends Callable[Int] {

  val config: Config = ConfigSource.resources("passulo.conf").loadOrThrow[Config]

  // noinspection VarCouldBeVal
  @Option(names = Array("-n", "--name"), description = Array("The name of the association as written on the pass."), required = true)
  var name: String = _

  // noinspection VarCouldBeVal
  @Option(names = Array("-kid", "--key-id"), description = Array("The key id."), required = true)
  var keyid: String = _

  // noinspection VarCouldBeVal
  @Option(names = Array("-s", "--server"), description = Array("The server to send the registration to."))
  var server: String = "passulo/Passulo-Server"

  @Option(names = Array("--public-key"), description = Array("The public key to register."))
  var publicKeyFile: File = _

  def call(): Int = {
    println("Loading public key…")
    val publicKeyFileRef = scala.Option(publicKeyFile).getOrElse(new File(config.keys.publicKey))
    val publicKey        = CryptoHelper.loadX509EncodedPEM(publicKeyFileRef)

    val baseURI = URI.create(s"https://github.com/$server/issues/new")
    val title   = "?title=" + URLEncoder.encode(s"New Public Key for `$name`", StandardCharsets.UTF_8)
    val body = "&body=" + URLEncoder.encode(
      s"""My Key-ID: `$keyid`
         |My key:
         |```
         |${CryptoHelper.encodeAsPEM(publicKey)}
         |```
         |My association: `$name`
         |
         |I can verify my identity in the following way: <please enter description>""".stripMargin,
      StandardCharsets.UTF_8
    )
    val url = baseURI.toURL.toString + title + body

    if (Desktop.isDesktopSupported)
      Desktop.getDesktop.browse(URI.create(url))

    println(StdOutText.headline(s"If your browser didn't open, please open this URL yourself:"))
    println(url)

    0
  }
}
