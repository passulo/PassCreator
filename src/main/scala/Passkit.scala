import Passulo.{IsoDateAtMidnightString, PassInfo, ShortDateString}
import de.brendamour.jpasskit.enums.PKDateStyle.*
import de.brendamour.jpasskit.enums.{PKBarcodeFormat, PKPassType}
import de.brendamour.jpasskit.passes.{PKGenericPass, PKGenericPassBuilder}
import de.brendamour.jpasskit.{PKBarcode, PKBarcodeBuilder, PKField, PKPass}

import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.PrivateKey

object Passkit {

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

  def barcode(passInfo: PassInfo, qrCodeSigningKey: PrivateKey, settings: Settings): PKBarcodeBuilder = {
    val token = Passulo.tokenFrom(passInfo, qrCodeSigningKey, settings.name)
    PKBarcode
      .builder()
      .format(PKBarcodeFormat.PKBarcodeFormatQR)
      .message(settings.server + "?code=" + token)
      .messageEncoding(StandardCharsets.ISO_8859_1)
  } // used by most scanners, according to Apple docs

  def pass(passInfo: PassInfo, qrCodeSigningKey: PrivateKey, config: Config): PKPass = PKPass
    .builder()
    .pass(content(passInfo, config.settings.name))
    .barcodeBuilder(barcode(passInfo, qrCodeSigningKey, config.settings))
    .formatVersion(1)
    .passTypeIdentifier(config.settings.identifier)
    .serialNumber(passInfo.number)
    .teamIdentifier(config.settings.team)
    .organizationName("Passulo")
    .description("$DESCRIPTION") // TODO i18n
    .backgroundColor(config.colors.backgroundColor)
    .foregroundColor(config.colors.foregroundColor)
    .labelColor(config.colors.labelColor)
    .webServiceURL(new URL(config.settings.server + "passes/"))
    .authenticationToken("vxwxd7J8AlNNFPS8k0a0FfUFtq0ewzFdc") // min 16 chars
    .appLaunchURL("passulo://")
    .associatedStoreIdentifier(1609117532)
//    .associatedApps() // for PassWallet on Android
    .voided(false)
    .build()
}
