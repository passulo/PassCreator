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

  val results = members.map { member =>
    val passId = NanoID.create()
    println(s"Creating pass for: ${member.fullName} (id=$passId)…")
    val qrCodeContent                = Passulo.createUrl(member, passId, privateKey, config.keys.keyIdentifier, config.passSettings)
    val pass                         = Passkit.pass(member, passId, qrCodeContent, config)
    val signedAndZippedPkPassArchive = Passkit4S.createSignedAndZippedPkPassArchive(pass, templateFolder, signingInformation)
    val filename                     = s"out/$passId-${member.filename}.pkpass"
    Files.createDirectories(Paths.get(filename).getParent)
    Files.write(Paths.get(filename), signedAndZippedPkPassArchive)
    println(s"Written to $filename")
    ResultListEntry.from(passId, member)
  }

  ResultListEntry.write(results.toSeq, "out/_results.csv")
}
