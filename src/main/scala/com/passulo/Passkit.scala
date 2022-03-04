package com.passulo

import de.brendamour.jpasskit.*
import de.brendamour.jpasskit.enums.PKDateStyle.*
import de.brendamour.jpasskit.enums.{PKBarcodeFormat, PKPassType, PKTextAlignment}
import de.brendamour.jpasskit.passes.{PKGenericPass, PKGenericPassBuilder}

import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.{LocalDate, ZoneId}
import scala.jdk.CollectionConverters.*
object Passkit {

  def IsoDateAtMidnightString(date: LocalDate): String = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant.toString

  def content(passInfo: PassInfo, passId: String, association: String): PKGenericPassBuilder =
    passInfo.template match {
      case "thumb"                 => content_thumb(passInfo).backFields(backfields(passInfo, passId, association).asJava)
      case other if !other.isBlank => content_strip(passInfo).backFields(backfields(passInfo, passId, association).asJava)
      case _                       => content_plain(passInfo).backFields(backfields(passInfo, passId, association).asJava)
    }

  def backfields(passInfo: PassInfo, passId: String, association: String): Seq[PKField] =
    Seq(
      field("name", "$ISSUED_TO", passInfo.fullName),
      field("company", "$COMPANY", passInfo.company),
      field("association", "$ASSOCIATION", association),
      field("number", "$NUMBER", passInfo.number),
      field_date("memberSince", "$MEMBER_SINCE", passInfo.memberSince),
      field("passId", "$PASSID", passId),
      field_date("createdAt", "$CREATED_AT", Some(LocalDate.now))
    ).flatten

  def field(key: String, label: String, value: String, alignment: PKTextAlignment = PKTextAlignment.PKTextAlignmentLeft): Option[PKField] =
    Some(PKField.builder().key(key).label(label).value(value).textAlignment(alignment).build())

  def field_date(key: String, label: String, value: Option[LocalDate]): Option[PKField] =
    value.map(date => PKField.builder().key(key).label(label).value(IsoDateAtMidnightString(date)).dateStyle(PKDateStyleShort).timeStyle(PKDateStyleNone).build())

  def content_plain(passInfo: PassInfo): PKGenericPassBuilder =
    PKGenericPass
      .builder()
      .passType(PKPassType.PKGenericPass)
      .headerFields(
        Seq(field_date("validity", "$DATEFIELD", passInfo.memberSince)).flatten.asJava
      )
      .primaryFields(
        Seq(field("name", "", passInfo.fullName)).flatten.asJava
      )
      .secondaryFields(
        Seq(
          field("company", "$COMPANY", passInfo.company),
          field("number", "$NUMBER", passInfo.number, PKTextAlignment.PKTextAlignmentRight)
        ).flatten.asJava
      )
      .auxiliaryFields(
        Seq(
          field("status", "$STATUS", passInfo.status),
          field("role", "$ROLE", passInfo.role, PKTextAlignment.PKTextAlignmentRight)
        ).flatten.asJava
      )

  def content_strip(passInfo: PassInfo): PKGenericPassBuilder =
    PKGenericPass
      .builder()
      .passType(PKPassType.PKStoreCard)
      .headerFields(
        Seq(field_date("validity", "$DATEFIELD", passInfo.memberSince)).flatten.asJava
      )
      .primaryFields(
        Seq(field("name", "", passInfo.fullName)).flatten.asJava
      )
      .secondaryFields(
        Seq(
          field("company", "$COMPANY", passInfo.company),
          field("number", "$NUMBER", passInfo.number, PKTextAlignment.PKTextAlignmentRight)
        ).flatten.asJava
      )

  def content_thumb(passInfo: PassInfo): PKGenericPassBuilder =
    PKGenericPass
      .builder()
      .passType(PKPassType.PKGenericPass)
      .headerFields(
        Seq(field("status", "$STATUS", passInfo.status)).flatten.asJava
      )
      .primaryFields(
        Seq(field("name", "", passInfo.fullName)).flatten.asJava
      )
      .secondaryFields(
        Seq(
          field("company", "$COMPANY", passInfo.company),
          field("number", "$NUMBER", passInfo.number, PKTextAlignment.PKTextAlignmentRight)
        ).flatten.asJava
      )
      .auxiliaryFields(
        Seq(
          field_date("validity", "$DATEFIELD", passInfo.memberSince),
          field("role", "$ROLE", passInfo.role, PKTextAlignment.PKTextAlignmentRight)
        ).flatten.asJava
      )

  def barcode(qrCodeContent: String): PKBarcodeBuilder =
    PKBarcode
      .builder()
      .format(PKBarcodeFormat.PKBarcodeFormatQR)
      .message(qrCodeContent)
      .messageEncoding(StandardCharsets.ISO_8859_1)
  // used by most scanners, according to Apple docs

  def beacons(beacons: Seq[Beacon]): Seq[PKBeacon] =
    beacons.map { config =>
      PKBeacon
        .builder()
        .proximityUUID(config.uuid)
        .relevantText(config.text)
        .build()
    }

  def pass(passInfo: PassInfo, passId: String, qrCodeContent: String, config: Config): PKPass = PKPass
    .builder()
    .pass(content(passInfo, passId, config.passSettings.associationName))
    .barcodeBuilder(barcode(qrCodeContent))
    .beacons(beacons(config.beacons).asJava)
    .formatVersion(1)
    .passTypeIdentifier(config.passSettings.identifier)
    .serialNumber(passInfo.number)
    .teamIdentifier(config.passSettings.team)
    .organizationName(config.passSettings.associationName)
    .description(config.passSettings.associationName)
    .backgroundColor(config.colors.backgroundColor)
    .foregroundColor(config.colors.foregroundColor)
    .labelColor(config.colors.labelColor)
    .webServiceURL(new URL(config.passSettings.server + "passes/"))
    .authenticationToken("vxwxd7J8AlNNFPS8k0a0FfUFtq0ewzFdc") // min 16 chars
    .appLaunchURL("passulo://")
    .associatedStoreIdentifier(1609117532)
//    .associatedApps() // for PassWallet on Android
    .voided(false)
    .build()
}
