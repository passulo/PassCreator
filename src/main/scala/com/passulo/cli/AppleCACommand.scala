package com.passulo.cli

import com.passulo.StdOutText
import picocli.CommandLine.Command

import java.util.concurrent.Callable

@Command(
  name = "appleCAappleCA",
  mixinStandardHelpOptions = true,
  description = Array("Downloads the Apple WWDR CA G4 CertificateApple WWDR CA G4 Certificate")
)
class AppleCACommand extends Callable[Int] {

  def call(): Int = {
    // TODO
    println(StdOutText.error("Not implemented yet."))
    0
  }
}
