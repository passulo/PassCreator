package com.passulo

import java.io.{File, FileInputStream, IOException}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.interfaces.EdECPublicKey
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyStore, PrivateKey, UnrecoverableKeyException}
import java.util.Base64
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Collection of helpful abstractions to load and decode certificates and keys, using java.Security and bouncy castle.
  *
  * helpful: https://www.baeldung.com/java-read-pem-file-keys helpful: https://www.tbray.org/ongoing/When/202x/2021/04/19/PKI-Detective
  */
object CryptoHelper {

  /** Reads an Ed25519 public key stored in X.509 Encoding (base64) in a PEM file (-----BEGIN … END … KEY-----) */
  def publicKeyFromFile(path: String): Option[EdECPublicKey] = {
    val file             = Source.fromFile(path)
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
    val file             = Source.fromFile(path)
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
  def privateKeyAndCertFromKeystore(keyStorePath: String, keyStorePassword: String): Option[(PrivateKey, X509Certificate)] =
    for {
      keyStoreInputStream <- getFileInputStream(keyStorePath).orElse(getFileInputStream(getClass.getClassLoader.getResource(keyStorePath).getFile))
      password             = keyStorePassword.toCharArray
      keystore             = KeyStore.getInstance("PKCS12")
      _ <- Try(keystore.load(keyStoreInputStream, password)) match {
             case Failure(e: IOException) if e.getCause.isInstanceOf[UnrecoverableKeyException] =>
               println("Wrong password for Keystore")
               None
             case Failure(exception) =>
               println(s"Error getting private key or certificate from keystore: ${exception.getMessage}")
               None
             case Success(value) => Some(value)
           }
      (privateKey, cert) <- extractCertificateWithKey(keystore, password)
    } yield (privateKey, cert)

  /** Reads an X.509 Certificate from a file */
  def certificateFromFile(certPath: String): Option[X509Certificate] =
    getFileInputStream(certPath).flatMap { inputStream =>
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

  private def getFileInputStream(path: String): Option[FileInputStream] = {
    val file = new File(path)
    if (file.exists()) {
      Some(new FileInputStream(file))
    } else None
  }
}
