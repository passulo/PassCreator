import dev.paseto.jpaseto.Pasetos
import kantan.csv.generic.*
import kantan.csv.java8.*
import kantan.csv.ops.*
import kantan.csv.rfc

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.time.{LocalDate, ZoneId}

object Passulo {

  case class PassInfo(
      id: String,
      firstName: String,
      middleName: String,
      lastName: String,
      gender: String,
      number: String,
      status: String,
      company: String,
      email: String,
      telephone: String,
      validUntil: LocalDate,
      memberSince: LocalDate
  ) {
    def fullName: String =
      (pronoun, NoneIfEmpty(firstName), NoneIfEmpty(middleName), NoneIfEmpty(lastName)) match {
        case (_, Some(f), Some(m), Some(l)) => s"$f $m $l"
        case (_, Some(f), None, Some(l))    => s"$f $l"
        case (Some(p), None, None, Some(l)) => s"$p $l"
        case (None, None, None, Some(l))    => s"$l"
        case _                              => "<no name>"
      }

    def pronoun: Option[String] =
      gender match {
        case "m" => Some("Mr.")
        case "f" => Some("Ms.")
        case "d" => Some("Mx.")
        case _   => None
      }

    def filename: String = java.net.URLEncoder.encode(s"$number-$fullName", StandardCharsets.UTF_8)
  }

  def IsoDateAtMidnightString(date: LocalDate): String = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant.toString
  def ShortDateString(date: LocalDate): String         = date.toString
  def NoneIfEmpty(string: String): Option[String]      = if (string.isBlank) None else Some(string)

  def tokenFrom(info: PassInfo, privateKey: PrivateKey, association: String): String = {
    // claims from https://paseto.io/rfc/ 6.1
    val token = Pasetos.V2.PUBLIC
      .builder()
      .setPrivateKey(privateKey)
      .claim("fna", info.firstName)
      .claim("mna", info.middleName)
      .claim("lna", info.lastName)
      .claim("gnd", info.gender)
      .claim("asn", association)
      .claim("num", info.number)
      .claim("sts", info.status)
      .claim("com", info.company)
      .claim("eml", info.email)
      .claim("tel", info.telephone)
      .claim("vut", ShortDateString(info.validUntil))
      // These fields make the token too big for a QR code
      //    .setExpiration(Instant.parse("2022-12-31T23:59:59.00Z"))
      //    .setIssuer("oma.jannikarndt.de")
      //    .setAudience("digitalcluster.hamburg")
      //    .setIssuedAt(Instant.now())
      //    .setSubject("membership")
      .compact()

    if (token.length > 340) {
      println(s"Token is too big for QR code! (length ${token.length})")
    }

    token
  }

  def readFromCsv(file: String): Iterable[PassInfo] =
    new File(file).asCsvReader[PassInfo](rfc.withHeader).toIterable.flatMap {
      case Left(value) =>
        println(s"Error reading row: $value")
        None
      case Right(value) => Some(value)
    }
}
