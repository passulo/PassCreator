package com.passulo
import com.google.protobuf.timestamp.Timestamp
import com.passulo.token.Token
import com.passulo.token.Token.Gender
import com.passulo.util.{CryptoHelper, NanoID}
import de.brendamour.jpasskit.signing.PKSigningInformation

import java.nio.file.{Files, Paths}
import java.security.PrivateKey
import java.time.{LocalTime, ZoneOffset}
import java.util.Base64

object Passulo {

  def createPasses(members: Iterable[PassInfo], signingInformation: PKSigningInformation, privateKey: PrivateKey, config: Config): Iterable[ResultListEntry] =
    members.map { member =>
      val passId                       = NanoID.create()
      val signedAndZippedPkPassArchive = createPass(member, passId, signingInformation, privateKey, config)
      val filename                     = s"out/$passId-${member.filename}.pkpass"
      Files.createDirectories(Paths.get(filename).getParent)
      Files.write(Paths.get(filename), signedAndZippedPkPassArchive)
      println(s"Written to $filename")
      ResultListEntry.from(passId, member)
    }

  def createPass(member: PassInfo, passId: String, signingInformation: PKSigningInformation, privateKey: PrivateKey, config: Config): Array[Byte] = {
    println(s"Creating pass for: ${member.fullName} (id=$passId) with template '${member.templateFolder}'…")
    val qrCodeContent                = Passulo.createUrl(member, passId, privateKey, config.keys.keyIdentifier, config.passSettings)
    val pass                         = Passkit.pass(member, passId, qrCodeContent, config)
    val templateFolder               = Paths.get(s"templates/${member.templateFolder}/").toAbsolutePath.toString
    val signedAndZippedPkPassArchive = Passkit4S.createSignedAndZippedPkPassArchive(pass, templateFolder, signingInformation)
    signedAndZippedPkPassArchive
  }

  def createUrl(info: PassInfo, passId: String, privateKey: PrivateKey, publicKeyIdentifier: String, settings: PassSettings): String = {
    val version = com.passulo.token.TokenProto.scalaDescriptor.packageName match {
      case "com.passulo.v1" => 1
      case other            => throw new RuntimeException(s"Unsupported Protobuf version for token: $other")
    }

    val tokenBytes       = createTokenBytes(info, passId, settings.associationName)
    val tokenEncoded     = Base64.getUrlEncoder.encodeToString(tokenBytes)
    val signature        = CryptoHelper.sign(tokenBytes, privateKey)
    val signatureEncoded = Base64.getUrlEncoder.encodeToString(signature)
    val url              = s"${settings.server}?code=$tokenEncoded&v=$version&sig=$signatureEncoded&kid=$publicKeyIdentifier"
    println(s"Created URL of length ${url.length}: $url")
    url
  }

  def createTokenBytes(info: PassInfo, passId: String, association: String): Array[Byte] = {

    val gender = info.gender match {
      case "m" | "male"          => Gender.male
      case "f" | "female"        => Gender.female
      case "d" | "x" | "diverse" => Gender.diverse
      case other                 => println(s"Cannot map gender $other!"); Gender.undefined
    }
    val token = Token(
      id = passId,
      firstName = info.firstName,
      middleName = info.middleName,
      lastName = info.lastName,
      gender = gender,
      number = info.number,
      status = info.status,
      company = info.company,
      email = info.email,
      telephone = info.telephone,
      association = association,
      validUntil = info.validUntil.map(d => Timestamp.of(d.toEpochSecond(LocalTime.MAX, ZoneOffset.UTC), nanos = 0)),
      memberSince = info.memberSince.map(d => Timestamp.of(d.toEpochSecond(LocalTime.MAX, ZoneOffset.UTC), nanos = 0))
    )
    token.toByteArray
  }
}
