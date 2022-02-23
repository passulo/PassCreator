package com.passulo
import com.passulo.token.Token
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, KeyPairGenerator}
import java.time.LocalDate
import java.util.Base64

class PassuloTest extends AnyWordSpec with Matchers {

  "Passulo" should {
    "create a valid token" in {

      // given
      val passInfo = PassInfo(
        id = "foobar",
        firstName = "Tanja",
        middleName = "",
        lastName = "Lange",
        gender = "f",
        number = "123",
        status = "gold",
        company = "foo",
        email = "lange@foo.com",
        telephone = "1234",
        validUntil = LocalDate.of(2022, 12, 31),
        memberSince = LocalDate.of(2015, 1, 1)
      )
      val associationName = "Passulo Test Assoc."

      // when
      val tokenBytes = Passulo.createTokenBytes(passInfo, associationName)

      // then
      val encodedToken = Base64.getUrlEncoder.encodeToString(tokenBytes)

      // must remain stable
      encodedToken shouldBe "CgZmb29iYXISBVRhbmphIgVMYW5nZSoBZjIDMTIzOgRnb2xkQgNmb29KDWxhbmdlQGZvby5jb21SBDEyMzRaE1Bhc3N1bG8gVGVzdCBBc3NvYy5iBgj_mcOdBmoGCP--l6UF"

      val parsed = Token.parseFrom(Base64.getUrlDecoder.decode(encodedToken))
      parsed.id shouldBe "foobar"
      parsed.firstName shouldBe "Tanja"
    }

    "create a valid signature" in {
      val privateKeyBytes = Base64.getDecoder.decode("MC4CAQAwBQYDK2VwBCIEIBY4OoD0rMpF72BIGUryoKa51W/vdgk4/dqy+WAquaq3")
      val privateKey      = KeyFactory.getInstance("ed25519").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))
      val signature       = Passulo.signToken(tokenBytes = "abc".getBytes, privateKey)

      val expected = "rWlQqQgZ8EGK++3HuCtTArg+z/cTiiew5haK2dyNh4QlKsq2NozvEdFw7gakTCb/k08k9v/Rhei0VopPZjEQCg=="

      Base64.getEncoder.encodeToString(signature) shouldBe expected
    }
  }
}