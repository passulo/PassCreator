import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.{AlgorithmIdentifier, SubjectPublicKeyInfo}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.io.pem.PemReader

import java.io.{File, FileInputStream, IOException, InputStreamReader}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyStore, PrivateKey, PublicKey, UnrecoverableKeyException}
import java.util.{Base64, HexFormat}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Collection of helpful abstractions to load and decode certificates and keys, using java.Security and bouncy castle.
  *
  * helpful: https://www.baeldung.com/java-read-pem-file-keys helpful: https://www.tbray.org/ongoing/When/202x/2021/04/19/PKI-Detective
  */
object CryptoHelper {

  /** Reads an Ed25519 public key stored in X.509 Encoding (base64) in a PEM file (-----BEGIN … END … KEY-----) */
  def publicKeyFromFile(path: String): PublicKey = {
    val pem        = new PemReader(new InputStreamReader(new FileInputStream(path)))
    val spec       = new X509EncodedKeySpec(pem.readPemObject().getContent)
    val keyFactory = KeyFactory.getInstance("ed25519", "BC")
    keyFactory.generatePublic(spec)
  }

  /** Reads an Ed25519 private key stored in PKCS #8 Encoding (base64) in a PEM file (-----BEGIN … END … KEY-----) */
  def privateKeyFromFile(path: String): PrivateKey = {
    val pem        = new PemReader(new InputStreamReader(new FileInputStream(path)))
    val spec       = new PKCS8EncodedKeySpec(pem.readPemObject().getContent)
    val keyFactory = KeyFactory.getInstance("ed25519", "BC")
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
             case Success(value) => Some(value)
           }
      (privateKey, cert) <- extractCertificateWithKey(keystore, password)
    } yield (privateKey, cert)

  /** Reads an X.509 Certificate from a file */
  def certificateFromFile(certPath: String): Option[X509Certificate] =
    getFileInputStream(certPath).flatMap { inputStream =>
      CertificateFactory.getInstance("X.509", "BC").generateCertificate(inputStream) match {
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

/** These used to make sense at some point, I don't remember why
  */
object CustomCryptoHackingDontUse {

  /** Reads an Ed25519 public key stored in X.509 Encoding (base64)
    *
    * Example: publicKeyFromBase64("MCowBQYDK2VwAyEAJuHkcaByMosGmA5LJfoSbkPaJ/YZ4eICEsDwwLRtN+I=")
    */
  def publicKeyFromBase64(base64String: String): PublicKey = publicKeyFromBytes(Base64.getDecoder.decode(base64String))

  /** Reads an Ed25519 public key stored in Hex Encoding
    *
    * Example: publicKeyFromHex("1eb9dbbbbc047c03fd70604e0071f0987e16b28b757225c11f00415d0e20b1a2")
    */
  def publicKeyFromHex(hex: String): PublicKey = publicKeyFromBytes(HexFormat.of().parseHex(hex))

  /** Extracts some magic bytes from the public key to… get a public key. */
  def publicKeyFromBytes(bytes: Array[Byte]): PublicKey = {
    val keyBytes  = Arrays.copyOfRange(bytes, 12, bytes.length)
    val algorithm = new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519)
    val keyInfo   = new SubjectPublicKeyInfo(algorithm, keyBytes)
    BouncyCastleProvider.getPublicKey(keyInfo)
  }

  def privateKeyFromBase64(base64String: String): PrivateKey = privateKeyFromBytes(Base64.getDecoder.decode(base64String))

  /** privateKeyFromHex("b4cbfb43df4ce210727d953e4a713307fa19bb7d9f85041438d9e11b942a37741eb9dbbbbc047c03fd70604e0071f0987e16b28b757225c11f00415d0e20b1a2")
    */
  def privateKeyFromHex(hex: String): PrivateKey = privateKeyFromBytes(HexFormat.of().parseHex(hex))

  def privateKeyFromBytes(bytes: Array[Byte]): PrivateKey = {
    val privateBytes = Arrays.copyOfRange(bytes, 16, 48)
    val algorithm    = new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519)
    val keyInfo      = new PrivateKeyInfo(algorithm, new DEROctetString(privateBytes))
    BouncyCastleProvider.getPrivateKey(keyInfo)
  }
}
