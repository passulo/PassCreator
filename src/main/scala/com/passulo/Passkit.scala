package com.passulo

import de.brendamour.jpasskit.enums.PKDateStyle.*
import de.brendamour.jpasskit.enums.{PKBarcodeFormat, PKPassType}
import de.brendamour.jpasskit.passes.{PKGenericPass, PKGenericPassBuilder}
import de.brendamour.jpasskit.*

import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.{LocalDate, ZoneId}
import scala.jdk.CollectionConverters.*
object Passkit {
  def IsoDateAtMidnightString(date: LocalDate): String = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant.toString
  def ShortDateString(date: LocalDate): String         = date.toString

  def content(passInfo: PassInfo, association: String): PKGenericPassBuilder = {
    val validUntil = IsoDateAtMidnightString(passInfo.validUntil)
    PKGenericPass
      .builder()
      .passType(PKPassType.PKStoreCard)
      .headerField(PKField.builder().key("validity").label("Gültig bis").value(validUntil).dateStyle(PKDateStyleShort).timeStyle(PKDateStyleNone).build())
      .primaryField(PKField.builder().key("identifier").value(passInfo.number).build())
      .secondaryField(PKField.builder().key("company").label(passInfo.company).value(passInfo.fullName).build())
      .auxiliaryField(PKField.builder().key("association").label("Verband").value(association).build())
      .auxiliaryField(PKField.builder().key("status").label("Status").value(passInfo.status).build())
      .backField(PKField.builder().key("name").label("Ausgestellt an").value(passInfo.fullName).build())
      .backField(PKField.builder().key("identifier").label("Mitgliedsnummer").value(passInfo.number).build())
      .backField(PKField.builder().key("company").label("Firma").value(passInfo.company).build())
      .backField(PKField.builder().key("since").label("Mitglied seit").value(ShortDateString(passInfo.memberSince)).build())
      .backField(PKField.builder().key("validUntil").label("Gültig bis").value(ShortDateString(passInfo.validUntil)).build())
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

  def pass(passInfo: PassInfo, qrCodeContent: String, config: Config): PKPass = PKPass
    .builder()
    .pass(content(passInfo, config.passSettings.associationName))
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
