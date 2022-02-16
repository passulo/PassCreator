import de.brendamour.jpasskit.signing.PKSigningInformation
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.nio.file.{Files, Paths}
import java.security.Security

object Main extends App {
  println("Setting up Security provider (Bouncy Castle)…")
  Security.addProvider(new BouncyCastleProvider)

  println("Loading configuration from 'passulo.conf'…")
  val config = Config.load

  println("Loading private key…")
  val pkFile     = getClass.getResource(config.keys.privateKey).getPath
  val privateKey = CryptoHelper.privateKeyFromFile(pkFile)

  println("Loading public key…")
  val pubkFile            = getClass.getResource(config.keys.publicKey).getPath
  val publicKey           = CryptoHelper.publicKeyFromFile(pubkFile)
  val publicKeyIdentifier = publicKey.map(_.getPoint.getY.toString).get.take(5)
  println(s"Identifier for this key: $publicKeyIdentifier")

  println("Loading Apple Developer certificate from keystore…")
  val keyStorePath = getClass.getResource(config.keys.keystore).getPath
  val (key, cert)  = CryptoHelper.privateKeyAndCertFromKeystore(keyStorePath, config.keys.password).get

  println("Loading Apple WWDR CA…")
  val appleWWDRCA = getClass.getResource(config.keys.appleCaCert).getPath
  val appleCert   = CryptoHelper.certificateFromFile(appleWWDRCA).get

  val signingInformation = new PKSigningInformation(cert, key, appleCert)

  val templateFolder = getClass.getClassLoader.getResource("template").getPath

  println(s"Loading Members from ${config.input.csv}")
  val membersPath = getClass.getResource(config.input.csv).getPath
  val members     = Passulo.readFromCsv(membersPath)

  members.foreach { member =>
    println(s"Creating pass for: ${member.fullName}…")
    val pass                         = Passkit.pass(member, privateKey, publicKeyIdentifier, config)
    val signedAndZippedPkPassArchive = Passkit4S.createSignedAndZippedPkPassArchive(pass, templateFolder, signingInformation)
    val filename                     = "out/" + member.filename + ".pkpass"
    Files.write(Paths.get(filename), signedAndZippedPkPassArchive)
    println(s"Written to $filename")

  }

}
