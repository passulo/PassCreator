package com.passulo.util

import java.io.{File, IOException}
import java.nio.file.Files
import java.security.*
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.interfaces.EdECPublicKey
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64
import scala.jdk.CollectionConverters.{CollectionHasAsScala, EnumerationHasAsScala}
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

  /** Reads a file in PEM format, returns the payload (between the lines) as bytes. */
  def decodePEM(file: File): Array[Byte] = {
    val encodedKeyString = loadPEM(file)
    Base64.getDecoder.decode(encodedKeyString)
  }

  /** Reads a file in PEM format, returns the payload (between the lines) as a base64 encoded String. */
  def loadPEM(file: File): String = Files.readAllLines(file.toPath).asScala.filterNot(_.startsWith("----")).mkString("")

  /** Reads a password-protected private key and matching certificate from a PKCS #12 keystore (.p12 file)
    *
    * @param keyStore
    *   Path to the .p12 file
    * @param keyStorePassword
    *   Password for the private key
    */
  def privateKeyAndCertFromKeystore(keyStore: File, keyStorePassword: String): Either[String, (PrivateKey, X509Certificate)] = {
    val password = keyStorePassword.toCharArray
    val keystore = KeyStore.getInstance("PKCS12")
    Try(keystore.load(keyStore.toURI.toURL.openStream(), password)) match {
      case Failure(e: IOException) if e.getCause.isInstanceOf[UnrecoverableKeyException] =>
        Left("Wrong password for Keystore")
      case Failure(exception) =>
        Left(s"Error getting private key or certificate from keystore: ${exception.getMessage}")
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

  private def extractCertificateWithKey(keyStore: KeyStore, password: Array[Char]): Either[String, (PrivateKey, X509Certificate)] =
    keyStore
      .aliases()
      .asScala
      .collectFirst { alias =>
        Try(keyStore.getKey(alias, password)) match {
          case Success(key: PrivateKey) =>
            keyStore.getCertificate(alias) match {
              case cert: X509Certificate =>
                Try(cert.checkValidity()) match {
                  case Failure(exception) => Left(s"Certificate is not valid: ${exception.toString}")
                  case Success(_)         => Right((key, cert))
                }
            }
          case Failure(e: UnrecoverableKeyException) => Left(s"Failed to load key from keystore: Is the password correct? ${e.toString}")
          case e                                     => Left(s"Failed to load key from keystore. $e")
        }
      }
      .getOrElse(Left("Error: The keystore doesn't contain any keys!"))

  def sign(bytes: Array[Byte], privateKey: PrivateKey): Array[Byte] = {
    val signature: Signature = Signature.getInstance("Ed25519")
    signature.initSign(privateKey)
    signature.update(bytes)
    signature.sign
  }
}
