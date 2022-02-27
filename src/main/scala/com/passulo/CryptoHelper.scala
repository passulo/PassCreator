package com.passulo

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.interfaces.{EdECPrivateKey, EdECPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.*
import java.util.Base64
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Collection of helpful abstractions to load and decode certificates and keys, using java.Security and bouncy castle.
  *
  * helpful: https://www.baeldung.com/java-read-pem-file-keys helpful: https://www.tbray.org/ongoing/When/202x/2021/04/19/PKI-Detective
  */
object CryptoHelper {

  def generateKeyPair(overwrite: Boolean, privateFile: String = "private.pem", publicFile: String = "public.pem"): Either[String, Success.type] = {
    val generator = KeyPairGenerator.getInstance("ed25519")
    val keypair   = generator.generateKeyPair()
    val publicKey = keypair.getPublic match {
      case publicKey: EdECPublicKey =>
        val encoded = Base64.getEncoder.encodeToString(publicKey.getEncoded)
        s"""-----BEGIN PUBLIC KEY-----
           |$encoded
           |-----END PUBLIC KEY-----""".stripMargin
    }
    val privateKey = keypair.getPrivate match {
      case privateKey: EdECPrivateKey =>
        val encoded = Base64.getEncoder.encodeToString(privateKey.getEncoded)
        s"""-----BEGIN PRIVATE KEY-----
           |$encoded
           |-----END PRIVATE KEY-----""".stripMargin
    }

    for {
      _ <- writeFile(privateFile, privateKey, overwrite)
      _ <- writeFile(publicFile, publicKey, overwrite)
    } yield Success
  }

  def writeFile(filename: String, text: String, overwrite: Boolean): Either[String, Success.type] = {
    val fileExists = Paths.get(filename).toFile.exists()
    if (fileExists && !overwrite) {
      Left(s"File $filename already exists, and you didn't specify to overwrite it. Stopping.")
    } else if (fileExists && overwrite) {
      println(s"File $filename already exists, overwriting…")
      val options = Seq(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.DSYNC, StandardOpenOption.TRUNCATE_EXISTING)
      Files.write(Paths.get(filename), text.getBytes(StandardCharsets.UTF_8), options*)
      Right(Success)
    } else {
      val options = Seq(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.DSYNC)
      Files.write(Paths.get(filename), text.getBytes(StandardCharsets.UTF_8), options*)
      Right(Success)
    }
  }

  /** Reads an Ed25519 public key stored in X.509 Encoding (base64) in a PEM file (-----BEGIN … END … KEY-----) */
  def publicKeyFromFile(path: String): Option[EdECPublicKey] = {
    val file             = Source.fromResource(path)
    val encodedKeyString = file.getLines().filterNot(_.startsWith("----")).mkString("")
    val keyBytes         = Base64.getDecoder.decode(encodedKeyString)
    val spec             = new X509EncodedKeySpec(keyBytes)
    val keyFactory       = KeyFactory.getInstance("ed25519")
    keyFactory.generatePublic(spec) match {
      case key: EdECPublicKey => Some(key)
    }
  }

  /** Reads an Ed25519 private key stored in PKCS #8 Encoding (base64) in a PEM file (-----BEGIN … END … KEY-----) */
  def privateKeyFromFile(path: String): PrivateKey = {
    val file             = Source.fromResource(path)
    val encodedKeyString = file.getLines().filterNot(_.startsWith("----")).mkString("")
    val keyBytes         = Base64.getDecoder.decode(encodedKeyString)
    val spec             = new PKCS8EncodedKeySpec(keyBytes)
    val keyFactory       = KeyFactory.getInstance("ed25519")
    keyFactory.generatePrivate(spec)
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
}
