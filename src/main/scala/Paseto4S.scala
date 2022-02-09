import dev.paseto.jpaseto.{Paseto, Pasetos}

import java.security.PublicKey

object Paseto4S {
  def parseToken(token: String, publicKey: PublicKey): Paseto = {
    val parser = Pasetos.parserBuilder().setPublicKey(publicKey).build()
    parser.parse(token)
  }
}
