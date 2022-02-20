package com.passulo

import de.brendamour.jpasskit.signing.PKSigningInformation
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.nio.file.{Files, Paths}

object Main extends App {
  println("Loading configuration from 'passulo.conf'…")
  val config = ConfigSource.resources("passulo.conf").loadOrThrow[Config]

  println("Loading private key…")
  val privateKey = CryptoHelper.privateKeyFromFile(config.keys.privateKey)

  println("Loading public key…")
  val publicKey = CryptoHelper.publicKeyFromFile(config.keys.publicKey)

  println("Loading Apple Developer certificate from keystore…")
  val (key, cert) = CryptoHelper.privateKeyAndCertFromKeystore(config.keys.keystore, config.keys.password).get

  println("Loading Apple WWDR CA…")
  val appleCert = CryptoHelper.certificateFromFile(config.keys.appleCaCert).get

  val signingInformation = new PKSigningInformation(cert, key, appleCert)

  val templateFolder = Paths.get("template/").toAbsolutePath.toString

  println(s"Loading Members from ${config.input.csv}")
  val members = PassInfo.readFromCsv(config.input.csv)

  members.foreach { member =>
    println(s"Creating pass for: ${member.fullName}…")
    val qrCodeContent                = Passulo.createUrl(member, privateKey, config.keys.keyIdentifier, config.passSettings)
    val pass                         = Passkit.pass(member, qrCodeContent, config)
    val signedAndZippedPkPassArchive = Passkit4S.createSignedAndZippedPkPassArchive(pass, templateFolder, signingInformation)
    val filename                     = "out/" + member.filename + ".pkpass"
    Files.write(Paths.get(filename), signedAndZippedPkPassArchive)
    println(s"Written to $filename")

  }

}
