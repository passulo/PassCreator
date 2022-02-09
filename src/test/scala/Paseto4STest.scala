import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import dev.paseto.jpaseto
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.Security

class Paseto4STest extends AnyWordSpec with Matchers {

  Security.addProvider(new BouncyCastleProvider)

  "Paseto4S" should {
    "parse a valid token" in {
      val validToken =
        "v2.public.eyJjb20iOiJVbml2ZXJzaXR5IG9mIElsbGlub2lzIGF0IENoaWNhZ28iLCJtbmEiOiJMLiIsInN0cyI6IlBsYXRpbiIsInZ1dCI6IjIwMjItMTItMzEiLCJudW0iOiI0MzgzOTIiLCJmbmEiOiJEYW5pZWwiLCJnbmQiOiJtIiwiYXNuIjoiSGFtYnVyZ0BXb3JrIiwibG5hIjoiQmVybnN0ZWluIn3U7SJEbragOHaCxIzyTc5uDHhtdyk1bpl3lxht5ubyyUXZyJ8hshTeDpsQFSipksVu8lTjgYwks3apxDMli7AD"
      val publicKey = CustomCryptoHackingDontUse.publicKeyFromBase64("MCowBQYDK2VwAyEAJuHkcaByMosGmA5LJfoSbkPaJ/YZ4eICEsDwwLRtN+I=")

      val parsedToken = Paseto4S.parseToken(validToken, publicKey)

      parsedToken.getVersion shouldBe jpaseto.Version.V2
      parsedToken.getPurpose shouldBe jpaseto.Purpose.PUBLIC
      val claims = parsedToken.getClaims
      claims.get("fna") shouldBe "Daniel"
      claims.get("mna") shouldBe "L."
      claims.get("lna") shouldBe "Bernstein"
      claims.get("gnd") shouldBe "m"
      claims.get("num") shouldBe "438392"
      claims.get("asn") shouldBe "Hamburg@Work"
      claims.get("com") shouldBe "University of Illinois at Chicago"
      claims.get("sts") shouldBe "Platin"
      claims.get("vut") shouldBe "2022-12-31"
    }
  }
}
