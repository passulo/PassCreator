package com.passulo.cli

import com.passulo.StdOutText
import com.passulo.util.CryptoHelper
import picocli.CommandLine.{Command, Option}

import java.util.concurrent.Callable
import scala.annotation.nowarn

@Command(
  name = "keypair",
  mixinStandardHelpOptions = true,
  description = Array("Creates a private and a public key based on the Ed25519 algorithm and writes them to 'private.pem' and 'public.pem'.")
)
class KeypairCommand extends Callable[Int] {

  // noinspection VarCouldBeVal
  @nowarn("msg=consider using immutable val")
  @Option(names = Array("-f", "--overwrite"), description = Array("Overwrite existing files"))
  private var overwrite = false

  def call(): Int = {
    // TODO: Load filenames from config; update config afterwards
    CryptoHelper.generateKeyPair(overwrite) match {
      case Left(errorMessage) => println(StdOutText.error(errorMessage))
      case Right(_)           => println(StdOutText.success("Written successfully"))
    }
    0
  }
}
