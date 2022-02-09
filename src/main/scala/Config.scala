import pureconfig.*
import pureconfig.generic.auto.*

object Config {
  def load: Config = ConfigSource.resources("passulo.conf").loadOrThrow[Config]
}

case class Config(keys: Keys, input: Input, settings: Settings, colors: Colors, ios: ios)

case class Input(csv: String)

case class Keys(privateKey: String, publicKey: String, keystore: String, password: String, appleCaCert: String)

case class Settings(
    name: String,
    server: String,
    team: String,
    identifier: String
)

case class Colors(
    foregroundColor: String,
    backgroundColor: String,
    labelColor: String
)

case class ios(associatedApp: Array[String])
