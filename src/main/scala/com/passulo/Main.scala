package com.passulo

import de.brendamour.jpasskit.signing.PKSigningInformation

import java.nio.file.{Files, Paths}

object Main extends App {
  println("Loading configuration from 'passulo.conf'…")
  val config = Config.load

  println("Loading private key…")
  val pkFile     = getClass.getClassLoader.getResource(config.keys.privateKey).getPath
  val privateKey = CryptoHelper.privateKeyFromFile(pkFile)

  println("Loading public key…")
  val pubkFile  = getClass.getClassLoader.getResource(config.keys.publicKey).getPath
  val publicKey = CryptoHelper.publicKeyFromFile(pubkFile)

  println("Loading Apple Developer certificate from keystore…")
  val keyStorePath = getClass.getClassLoader.getResource(config.keys.keystore).getPath
  val (key, cert)  = CryptoHelper.privateKeyAndCertFromKeystore(keyStorePath, config.keys.password).get

  println("Loading Apple WWDR CA…")
  val appleWWDRCA = getClass.getClassLoader.getResource(config.keys.appleCaCert).getPath
  val appleCert   = CryptoHelper.certificateFromFile(appleWWDRCA).get

  val signingInformation = new PKSigningInformation(cert, key, appleCert)

  val templateFolder = getClass.getClassLoader.getResource("template").getPath

  println(s"Loading Members from ${config.input.csv}")
  val membersPath = getClass.getClassLoader.getResource(config.input.csv).getPath
  val members     = PassInfo.readFromCsv(membersPath)

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
