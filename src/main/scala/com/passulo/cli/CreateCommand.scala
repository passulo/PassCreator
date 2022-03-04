package com.passulo.cli

import cats.implicits.toBifunctorOps
import com.passulo.*
import com.passulo.cli.CreateCommand.{loadSigningInfo, validatePublicKey}
import com.passulo.util.{CryptoHelper, FileOperations, Http}
import de.brendamour.jpasskit.signing.PKSigningInformation
import picocli.CommandLine.{Command, Option}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.io.File
import java.util.concurrent.Callable
import scala.util.{Failure, Success, Try}

@Command(
  name = "create",
  mixinStandardHelpOptions = true,
  description = Array("Loads members and creates Passulo-passes for them.")
)
class CreateCommand extends Callable[Int] {

  // noinspection VarCouldBeVal
  @Option(names = Array("-s", "--source"), description = Array("CSV files with members."), required = true)
  var source: File = _

  // noinspection VarCouldBeVal
  @Option(names = Array("-n", "--dry-run"), description = Array("Don't actually register the passes at the server."))
  var dryRun: Boolean = false

  def call(): Int = {
    createPasses() match {
      case Left(value)  => println(StdOutText.error(s"Error creating passes: ${value.message}"))
      case Right(value) => println(StdOutText.success(s"Successfully created ${value.size} passes!"))
    }
    println(StdOutText.success("Done."))
    0
  }

  def createPasses(): Either[PassCreationError, Iterable[ResultListEntry]] =
    for {
      configFile         <- FileOperations.loadFile("passulo.conf")
      config             <- ConfigSource.file(configFile).load[Config].toOption.toRight(ConfigurationError("Failed to load configuration."))
      privateKeyFile     <- FileOperations.loadFile(config.keys.privateKey)
      privateKey         <- Try(CryptoHelper.loadPKCS8EncodedPEM(privateKeyFile)).toOption.toRight(KeyError("Failed to parse private key."))
      signingInformation <- loadSigningInfo(config.keys)
      publicKeyFile      <- FileOperations.loadFile(config.keys.publicKey)
      _                  <- validatePublicKey(config, publicKeyFile, dryRun)
      _                   = println(s"Loading Members from ${source.getAbsolutePath}")
      members            <- PassInfo.readFromCsv(source).leftMap[PassCreationError](e => CsvError(e.getMessage))
      _                   = println("Creating passes")
      results             = Passulo.createPasses(members, signingInformation, privateKey, config)
      _                   = ResultListEntry.write(results.toSeq, "out/_results.csv")
    } yield results

}

object CreateCommand {
  def loadSigningInfo(keys: Keys): Either[PassCreationError, PKSigningInformation] =
    for {
      keystoreFile <- FileOperations.loadFile(keys.keystore)
      tuple        <- CryptoHelper.privateKeyAndCertFromKeystore(keystoreFile, keys.password).leftMap(KeyError)
      (key, cert)   = (tuple._1, tuple._2)
      appleCert     = CryptoHelper.certificateFromFile(keys.appleCaCert).get
    } yield new PKSigningInformation(cert, key, appleCert)

  def validatePublicKey(config: Config, publicKeyFile: File, dryRun: Boolean): Either[PassCreationError, Success.type] = {
    val localPublicKey = CryptoHelper.loadPEM(publicKeyFile)

    println(s"Checking public key is registered with server ${config.passSettings.server}…")
    Try(Http.get(config.passSettings.server + "v1/key/" + config.keys.keyIdentifier)) match {
      case Success(response) if response.statusCode() == 200 =>
        val serverKey = response.body().stripPrefix("\"").stripSuffix("\"")
        println(s"Key with id ${config.keys.keyIdentifier} is registered with server.")
        println(s"server: $serverKey")
        println(s"local:  $localPublicKey")
        if (serverKey == localPublicKey) {
          println(StdOutText.success("Public Key matches registered key."))
          Right(Success)
        } else if (dryRun) {
          println(StdOutText.warn("Public Key is not registered with server. Ignoring check because --dry-run is specified."))
          Right(Success)
        } else {
          Left(PublicKeyNotOnServer("Public Key does not match the one registered with the server. Use --dry-run to skip this check."))
        }
      case Success(response) if response.statusCode() == 404 && dryRun =>
        println(StdOutText.warn("Public Key is not registered with server. Ignoring check because --dry-run is specified."))
        Right(Success)
      case _ if dryRun =>
        println(StdOutText.warn("Could not verify if Public Key is registered with server. Ignoring check because --dry-run is specified."))
        Right(Success)

      case Success(response) if response.statusCode() == 404 =>
        Left(ServerError(s"""Key for keyId ${config.keys.keyIdentifier} was not found on server ${config.passSettings.server}.
                            |Make sure you have registered with the server (using the register command).
                            |You can use --dry-run to skip this check.""".stripMargin))
      case Success(response) =>
        Left(ServerError(s"Error from server (${response.statusCode()}): ${response.body()}. Use --dry-run to skip this check."))
      case Failure(exception) =>
        Left(ServerError(s"""An exception occurred when asking the server at ${config.passSettings.server}.
                            |Make sure you are connected to the internet.
                            |You can use --dry-run to skip this check.
                            |Details: ${exception.toString}""".stripMargin))
    }
  }
}

trait PassCreationError {
  def message: String
}

case class ConfigurationError(message: String)   extends PassCreationError
case class FileNotFound(message: String)         extends PassCreationError
case class KeyError(message: String)             extends PassCreationError
case class ServerError(message: String)          extends PassCreationError
case class CsvError(message: String)             extends PassCreationError
case class PublicKeyNotOnServer(message: String) extends PassCreationError
