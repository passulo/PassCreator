package com.passulo.cli

import com.passulo.StdOutText
import picocli.CommandLine.{Command, Option}

import java.util.concurrent.Callable

@Command(
  name = "certificate",
  mixinStandardHelpOptions = true,
  description = Array("Creates a Signing Certificate Request to submit to Apple.")
)
class CertificateCommand extends Callable[Int] {

  // noinspection VarCouldBeVal
  @Option(names = Array("-e", "--e-mail", "--email"), description = Array("Your e-mail-address."), required = true)
  var name: String = _

  def call(): Int = {
    // TODO
    println(StdOutText.error("Not implemented yet."))
    0
  }
}
