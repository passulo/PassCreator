package com.passulo.cli

import com.passulo.*
import com.passulo.util.CryptoHelper
import de.brendamour.jpasskit.signing.PKSigningInformation
import picocli.CommandLine.{Command, Option}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.io.File
import java.security.PrivateKey
import java.util.concurrent.Callable

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
    val privateKey: PrivateKey = CryptoHelper.loadPKCS8EncodedPEM(new File(config.keys.privateKey))

    val signingInformation = loadSigningInfo(config.keys)

    println(s"Loading Members from ${config.input.csv}")
    val members: Iterable[PassInfo] = PassInfo.readFromCsv(config.input.csv)

    val results = Passulo.createPasses(members, signingInformation, privateKey, config)
    ResultListEntry.write(results.toSeq, "out/_results.csv")
  }
}
