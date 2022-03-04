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

  @Option(names = Array("-e", "--e-mail", "--email"), description = Array("Your e-mail-address."))
  var name: String = _

  def call(): Int = {
    // TODO
    println(StdOutText.error("This is not implemented yet."))
    println("Use Keychain to create a certificate signing request.")
    println("Then create a Pass Identifier in your Apple Developer Account:")
    println("https://developer.apple.com/account/resources/identifiers/list/passTypeId (beginning with 'pass.')")
    println("Next, create a new 'Pass Type ID Certificate' at https://developer.apple.com/account/resources/certificates/add")
    println("Upload the request. Afterwards, download the certificate and import it into your keychain.")
    println("Then select the certificate and the key and export it as a keystore (`.p12`).")
    println("Choose a secure password and add it to `passulo.conf`.")
    0
  }
}
