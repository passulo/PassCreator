package com.passulo

import java.io.{File, IOException}
import java.nio.file.Files
import java.security.*
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.interfaces.EdECPublicKey
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Collection of helpful abstractions to load and decode certificates and keys, using java.Security and bouncy castle.
  *
  * helpful: https://www.baeldung.com/java-read-pem-file-keys helpful: https://www.tbray.org/ongoing/When/202x/2021/04/19/PKI-Detective
  */
object CryptoHelper {

  def generateKeyPair(overwrite: Boolean, privateFile: String = "private.pem", publicFile: String = "public.pem"): Either[String, Success.type] = {
    val generator  = KeyPairGenerator.getInstance("ed25519")
    val keypair    = generator.generateKeyPair()
    val publicKey  = encodeAsPEM(keypair.getPublic)
    val privateKey = encodeAsPEM(keypair.getPrivate)

    for {
      _ <- FileOperations.writeFile(privateFile, privateKey, overwrite)
      _ <- FileOperations.writeFile(publicFile, publicKey, overwrite)
    } yield Success
  }

  def encodeAsPEM(key: Key): String = {
    val identifier = key match {
      case _: PrivateKey => "PRIVATE"
      case _: PublicKey  => "PUBLIC"
      case key           => throw new UnknownError(s"Unknown key type: ${key.getFormat}")
    }
    val encoded = Base64.getEncoder.encodeToString(key.getEncoded)
    s"""-----BEGIN $identifier KEY-----
       |$encoded
       |-----END $identifier KEY-----""".stripMargin
  }

  /** Reads an Ed25519 public key stored in X.509 Encoding (base64) in a PEM file (-----BEGIN … END … KEY-----) */
  def loadX509EncodedPEM(file: File): EdECPublicKey = {
    val keyBytes   = decodePEM(file)
    val spec       = new X509EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance("ed25519")
    keyFactory.generatePublic(spec) match {
      case key: EdECPublicKey => key
    }
  }

  /** Reads an Ed25519 private key stored in PKCS #8 Encoding (base64) in a PEM file (-----BEGIN … END … KEY-----) */
  def loadPKCS8EncodedPEM(file: File): PrivateKey = {
    val keyBytes   = decodePEM(file)
    val spec       = new PKCS8EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance("ed25519")
    keyFactory.generatePrivate(spec)
  }

  /** Reads a file in PEM format, returns the payload (between the lines). */
  def decodePEM(filename: String): Array[Byte] = {
    val file             = FileOperations.loadFile(filename)
    val encodedKeyString = file.getLines().filterNot(_.startsWith("----")).mkString("")
    Base64.getDecoder.decode(encodedKeyString)
  }

  /** Reads a file in PEM format, returns the payload (between the lines). */
  def decodePEM(file: File): Array[Byte] = {
    val encodedKeyString = Files.readAllLines(file.toPath).asScala.filterNot(_.startsWith("----")).mkString("")
    Base64.getDecoder.decode(encodedKeyString)
  }

  /** Reads a password-protected private key and matching certificate from a PKCS #12 keystore (.p12 file)
    *
    * @param keyStorePath
    *   Path to the .p12 file
    * @param keyStorePassword
    *   Password for the private key
    */
  def privateKeyAndCertFromKeystore(keyStorePath: String, keyStorePassword: String): Option[(PrivateKey, X509Certificate)] = {
    val inputStream = Thread.currentThread().getContextClassLoader.getResourceAsStream(keyStorePath)
    val password    = keyStorePassword.toCharArray
    val keystore    = KeyStore.getInstance("PKCS12")
    Try(keystore.load(inputStream, password)) match {
      case Failure(e: IOException) if e.getCause.isInstanceOf[UnrecoverableKeyException] =>
        println("Wrong password for Keystore")
        None
      case Failure(exception) =>
        println(s"Error getting private key or certificate from keystore: ${exception.getMessage}")
        None
      case Success(_) => extractCertificateWithKey(keystore, password)
    }
  }

  /** Reads an X.509 Certificate from a file */
  def certificateFromFile(certPath: String): Option[X509Certificate] = {
    val inputStream = Thread.currentThread().getContextClassLoader.getResourceAsStream(certPath)
    CertificateFactory.getInstance("X.509").generateCertificate(inputStream) match {
      case cert: X509Certificate if Try(cert.checkValidity()).isSuccess => Some(cert)
      case _                                                            => None
    }
  }

  private def extractCertificateWithKey(keyStore: KeyStore, password: Array[Char]): Option[(PrivateKey, X509Certificate)] =
    keyStore.aliases().asScala.collectFirst { alias =>
      keyStore.getKey(alias, password) match {
        case key: PrivateKey =>
          keyStore.getCertificate(alias) match {
            case cert: X509Certificate if Try(cert.checkValidity()).isSuccess => (key, cert)
          }
      }

    }

  def sign(bytes: Array[Byte], privateKey: PrivateKey): Array[Byte] = {
    val signature: Signature = Signature.getInstance("Ed25519")
    signature.initSign(privateKey)
    signature.update(bytes)
    signature.sign
  }
}
