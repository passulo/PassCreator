package com.passulo.cli

import com.passulo.StdOutText
import picocli.CommandLine.{Command, Option}

import java.util.concurrent.Callable

@Command(
  name = "register",
  mixinStandardHelpOptions = true,
  description = Array("Registers the public key with a Passulo-Server.")
)
class RegisterCommand extends Callable[Int] {

  // noinspection VarCouldBeVal
  @Option(names = Array("-n", "--names"), description = Array("The text to sign."), required = true)
  var name: String = _

  // noinspection VarCouldBeVal
  @Option(names = Array("-s", "--server"), description = Array("The server to send the registration to."), required = true)
  var server: String = _

  def call(): Int = {
    //    println("Loading public key…")
    //    val publicKey = CryptoHelper.publicKeyFromFile(config.keys.publicKey)

    // TODO
    println(StdOutText.error("Not implemented yet."))
    0
  }
}
