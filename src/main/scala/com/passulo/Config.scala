package com.passulo

case class Config(keys: Keys, input: Input, passSettings: PassSettings)
case class Style(colors: Colors, format: String, beacons: Seq[Beacon])

case class Input(csv: String)

case class Keys(privateKey: String, publicKey: String, keyIdentifier: String, keystore: String, password: String, appleCaCert: String)

case class PassSettings(
    associationName: String,
    server: String,
    team: String,
    identifier: String
)

case class Colors(
    foregroundColor: String,
    backgroundColor: String,
    labelColor: String
)

case class Beacon(uuid: String, text: String)
