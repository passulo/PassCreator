package com.passulo

import de.brendamour.jpasskit.enums.PKDateStyle.*
import de.brendamour.jpasskit.enums.{PKBarcodeFormat, PKPassType, PKTextAlignment}
import de.brendamour.jpasskit.passes.{PKGenericPass, PKGenericPassBuilder}
import de.brendamour.jpasskit.*

import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.{LocalDate, ZoneId}
import scala.jdk.CollectionConverters.*
object Passkit {
  def IsoDateAtMidnightString(date: LocalDate): String = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant.toString
  def ShortDateString(date: LocalDate): String         = date.toString

  def content(passInfo: PassInfo, passId: String, association: String): PKGenericPassBuilder =
    passInfo.template match {
      case "thumb"                      => content_thumb(passInfo).backFields(backfields(passInfo, passId, association).asJava)
      case "silver" | "gold" | "platin" => content_strip(passInfo).backFields(backfields(passInfo, passId, association).asJava)
      case _                            => content_plain(passInfo).backFields(backfields(passInfo, passId, association).asJava)
    }

  def backfields(passInfo: PassInfo, passId: String, association: String): Seq[PKField] = {
    val memberSinceText = passInfo.memberSince.map(d => s"\nMitglied seit: ${ShortDateString(d)}").getOrElse("")
    Seq(
      PKField.builder().key("name").label("Ausgestellt an").value(passInfo.fullName).build(),
      PKField.builder().key("company").label("Firma").value(passInfo.company).build(),
      PKField.builder().key("association").value(s"Verband: $association\nMitgliedsnummer: ${passInfo.number}$memberSinceText").build(),
      PKField.builder().key("passId").label("Pass").value(s"ID: $passId\nErstellt am ${ShortDateString(LocalDate.now())}").build()
    )
  }

  def content_plain(passInfo: PassInfo): PKGenericPassBuilder = {
    val validFrom = passInfo.memberSince.map(IsoDateAtMidnightString)
    val pass = PKGenericPass
      .builder()
      .passType(PKPassType.PKGenericPass)
      .headerField(
        validFrom
          .map(date => PKField.builder().key("validity").label("Gültig ab").value(date).dateStyle(PKDateStyleShort).timeStyle(PKDateStyleNone).build())
          .getOrElse(PKField.builder().build())
      )
      .primaryField(PKField.builder().key("name").value(passInfo.fullName).build())
      .secondaryField(PKField.builder().key("company").label("Firma").value(passInfo.company).build())
      .secondaryField(PKField.builder().key("number").label("Mitgliedsnummer").value(passInfo.number).textAlignment(PKTextAlignment.PKTextAlignmentRight).build())
      .auxiliaryField(PKField.builder().key("status").label("status").value(passInfo.status).build())

    if (!passInfo.role.isBlank)
      pass.auxiliaryField(PKField.builder().key("role").label("Funktion").value(passInfo.role).textAlignment(PKTextAlignment.PKTextAlignmentRight).build())
    else
      pass
  }

  def content_strip(passInfo: PassInfo): PKGenericPassBuilder = {
    val validFrom = passInfo.memberSince.map(IsoDateAtMidnightString)
    PKGenericPass
      .builder()
      .passType(PKPassType.PKStoreCard)
      .headerField(
        validFrom
          .map(date => PKField.builder().key("validity").label("Gültig ab").value(date).dateStyle(PKDateStyleShort).timeStyle(PKDateStyleNone).build())
          .getOrElse(PKField.builder().build())
      )
      .primaryField(PKField.builder().key("name").value(passInfo.fullName).build())
      .secondaryField(PKField.builder().key("company").label("Firma").value(passInfo.company).build())
      .secondaryField(PKField.builder().key("number").label("Mitgliedsnummer").value(passInfo.number).textAlignment(PKTextAlignment.PKTextAlignmentRight).build())
  }

  def content_thumb(passInfo: PassInfo): PKGenericPassBuilder = {
    val validFrom = passInfo.memberSince.map(IsoDateAtMidnightString)
    PKGenericPass
      .builder()
      .passType(PKPassType.PKGenericPass)
      .headerField(PKField.builder().key("status").label("Status").value(passInfo.status).build())
      .primaryField(PKField.builder().key("name").value(passInfo.fullName).build())
      .secondaryField(PKField.builder().key("company").label("Firma").value(passInfo.company).build())
      .secondaryField(PKField.builder().key("number").label("Mitgliedsnummer").value(passInfo.number).textAlignment(PKTextAlignment.PKTextAlignmentRight).build())
      .auxiliaryField(
        validFrom
          .map(date => PKField.builder().key("validity").label("Gültig ab").value(date).dateStyle(PKDateStyleShort).timeStyle(PKDateStyleNone).build())
          .getOrElse(PKField.builder().build())
      )
      .auxiliaryField(PKField.builder().key("function").label("Funktion").value("Kassenwart").textAlignment(PKTextAlignment.PKTextAlignmentRight).build())
  }

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
    .description("$DESCRIPTION") // TODO i18n
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
