package com.passulo

import de.brendamour.jpasskit.signing.PKSigningInformation
import picocli.CommandLine
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.{usage, Command, Option, Parameters}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.security.PrivateKey
import java.util.Base64
import java.util.concurrent.Callable
import scala.annotation.nowarn

object Main {

  def main(args: Array[String]): Unit = {
    new CommandLine(TopCommand).execute(args*)

    ()
  }
}

object StdOutText {
  def error(text: String): String   = Ansi.AUTO.string(s"@|bold,red $text|@")
  def success(text: String): String = Ansi.AUTO.string(s"@|bold,green $text|@")
}

@Command(
  name = "",
  subcommands = Array(
    classOf[KeypairCommand],
    classOf[SignCommand],
    classOf[RegisterCommand],
    classOf[CertificateCommand],
    classOf[AppleCACommand],
    classOf[TemplateCommand],
    classOf[CreateCommand]
  ),
  mixinStandardHelpOptions = true,
  version = Array("@|bold,blue PassCreator v1|@", "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})", "OS: ${os.name} ${os.version} ${os.arch}"),
  description = Array("Creates Membership Passes for Passulo. Check out https://www.passulo.com")
)
object TopCommand extends Callable[Int] {
  def call(): Int = {
    usage(this, System.out)
    0
  }
}

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
  var privateKeyFile: String = _

  def call(): Int = {
    val config     = ConfigSource.resources("passulo.conf").loadOrThrow[Config]
    val privateKey = CryptoHelper.privateKeyFromFile(config.keys.privateKey)

    val message = text.getBytes
    val signed  = Passulo.signToken(message, privateKey)
    val encoded = Base64.getUrlEncoder.encodeToString(signed)
    println(encoded)
    0
  }
}

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

@Command(
  name = "template",
  mixinStandardHelpOptions = true,
  description = Array("Creates a template-folder.")
)
class TemplateCommand extends Callable[Int] {

  def call(): Int = {
    // TODO
    println(StdOutText.error("Not implemented yet."))
    0
  }
}

@Command(
  name = "create",
  mixinStandardHelpOptions = true,
  description = Array("Loads members and creates Passulo-passes for them.")
)
class CreateCommand extends Callable[Int] {

  // noinspection VarCouldBeVal
  @Option(names = Array("-s", "--source"), description = Array("CSV files with members."), required = true)
  var source: String = _

  def call(): Int = {
    createPasses()
    println(StdOutText.success("Done."))
    0
  }

  def loadSigningInfo(keys: Keys): PKSigningInformation = {
    println("Loading Apple Developer certificate from keystore…")
    val (key, cert) = CryptoHelper.privateKeyAndCertFromKeystore(keys.keystore, keys.password).get

    println("Loading Apple WWDR CA…")
    val appleCert = CryptoHelper.certificateFromFile(keys.appleCaCert).get

    new PKSigningInformation(cert, key, appleCert)
  }

  def createPasses(): Unit = {
    println("Loading configuration from 'passulo.conf'…")
    val config: Config = ConfigSource.resources("passulo.conf").loadOrThrow[Config]

    println("Loading private key…")
    val privateKey: PrivateKey = CryptoHelper.privateKeyFromFile(config.keys.privateKey)

    val signingInformation = loadSigningInfo(config.keys)

    println(s"Loading Members from ${config.input.csv}")
    val members: Iterable[PassInfo] = PassInfo.readFromCsv(config.input.csv)

    val results = Passulo.createPasses(members, signingInformation, privateKey, config)
    ResultListEntry.write(results.toSeq, "out/_results.csv")
  }
}
