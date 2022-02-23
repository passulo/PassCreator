package com.passulo
import com.google.protobuf.timestamp.Timestamp
import com.passulo.token.Token
import com.passulo.token.Token.Gender

import java.security.{PrivateKey, Signature}
import java.time.{LocalTime, ZoneOffset}
import java.util.Base64

object Passulo {

  def createUrl(info: PassInfo, passId: String, privateKey: PrivateKey, publicKeyIdentifier: String, settings: PassSettings): String = {
    val version = com.passulo.token.TokenProto.scalaDescriptor.packageName match {
      case "com.passulo.v1" => 1
      case other            => throw new RuntimeException(s"Unsupported Protobuf version for token: $other")
    }

    val tokenBytes       = createTokenBytes(info, passId, settings.associationName)
    val tokenEncoded     = Base64.getUrlEncoder.encodeToString(tokenBytes)
    val signature        = signToken(tokenBytes, privateKey)
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
      validUntil = Some(Timestamp.of(info.validUntil.toEpochSecond(LocalTime.MAX, ZoneOffset.UTC), nanos = 0)),
      memberSince = Some(Timestamp.of(info.memberSince.toEpochSecond(LocalTime.MAX, ZoneOffset.UTC), nanos = 0))
    )
    println(s"Token length: ${token.toByteArray.length} bytes")
    token.toByteArray
  }

  def signToken(tokenBytes: Array[Byte], privateKey: PrivateKey): Array[Byte] = {
    val signature: Signature = Signature.getInstance("Ed25519")
    signature.initSign(privateKey)
    signature.update(tokenBytes)
    val signatureBytes = signature.sign
    println(s"Signature length: ${signatureBytes.length} bytes")
    signatureBytes
  }
}
