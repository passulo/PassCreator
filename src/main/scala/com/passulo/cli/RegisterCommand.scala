package com.passulo.cli

import com.passulo.util.{CryptoHelper, Http}
import com.passulo.{Config, StdOutText}
import io.circe.generic.auto.*
import io.circe.syntax.*
import picocli.CommandLine.{Command, Option}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.awt.Desktop
import java.io.File
import java.net.URI
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
  var server: String = "app.passulo.com"

  @Option(names = Array("--public-key"), description = Array("The public key to register."))
  var publicKeyFile: File = _

  def call(): Int = {
    println("Loading public key…")
    val publicKeyFileRef = scala.Option(publicKeyFile).getOrElse(new File(config.keys.publicKey))
    val publicKey        = CryptoHelper.loadX509EncodedPEM(publicKeyFileRef)
    val url              = s"https://$server/v1/key/register"
    val payload          = RegisterKey(keyid, name, CryptoHelper.encodeAsPEM(publicKey)).asJson
    println(s"Asking server at $url what to do…")

    val serverCommand = Http.post(url, payload)
    val goTo          = serverCommand.body()

    serverCommand.statusCode() match {
      case 200 =>
        if (Desktop.isDesktopSupported)
          Desktop.getDesktop.browse(URI.create(goTo.stripPrefix("\"").stripSuffix("\"")))

        println(StdOutText.headline(s"If your browser didn't open, please open this URL yourself:"))
        println(goTo)

      case _ =>
        println(StdOutText.error(s"Error asking the server $url what to do."))
        println(s"Response was ${serverCommand.statusCode()} ${serverCommand.body()}")
    }

    0
  }

  case class RegisterKey(keyId: String, association: String, key: String)
}
