package com.passulo.cli

import com.passulo.CryptoHelper
import picocli.CommandLine.{Command, Option, Parameters}

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.Callable

@Command(
  name = "sign",
  mixinStandardHelpOptions = true,
  description = Array("Uses the private key to sign text.")
)
class SignCommand extends Callable[Int] {

  // noinspection VarCouldBeVal
  @Parameters(index = "0", description = Array("The text to sign."))
  var text: String = _

  @Option(names = Array("-k", "--private-key"), description = Array("Path to the private key file."))
  var privateKeyFile: File = new File("private.pem")

  @Option(names = Array("--url-encoded"), description = Array("Use Base64url encoding for output."))
  var urlEncoded: Boolean = false

  def call(): Int = {
    val privateKey = CryptoHelper.loadPKCS8EncodedPEM(privateKeyFile)

    val message = text.getBytes(StandardCharsets.UTF_8)
    val signed  = CryptoHelper.sign(message, privateKey)
    val encoded = urlEncoded match {
      case true  => Base64.getUrlEncoder.encodeToString(signed)
      case false => Base64.getEncoder.encodeToString(signed)
    }

    println(encoded)
    0
  }
}
