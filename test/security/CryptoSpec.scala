package security

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import utils.OCGConfiguration
import org.scalatestplus.junit.JUnitRunner
import play.api.Configuration


@RunWith(classOf[JUnitRunner])
class CryptoSpec extends AnyWordSpec with Matchers with MockitoSugar {

  val secretKey = "test-secret-key-for-hmac-verification"

  val conf: Configuration = Configuration(ConfigFactory.load)
  val ocgConfig: OCGConfiguration = new OCGConfiguration(Configuration(
    conf.underlying.withValue("play.http.secret.key", ConfigValueFactory.fromAnyRef(secretKey))
  ))

  val crypto = new Crypto(ocgConfig)

  "Crypto" should {

    "sign a message consistently" in {
      val message = "test-message"
      val signature1 = crypto.sign(message)
      val signature2 = crypto.sign(message)

      signature1 shouldBe signature2
      signature1 should not be empty
    }

    "verify a correct signature" in {
      val message = "test-message"
      val signature = crypto.sign(message)

      crypto.verify(message, signature) shouldBe true
    }

    "reject an incorrect signature" in {
      val message = "test-message"
      val wrongMessage = "wrong-message"
      val signature = crypto.sign(message)

      crypto.verify(wrongMessage, signature) shouldBe false
    }

    "reject a tampered signature" in {
      val message = "test-message"
      val signature = crypto.sign(message)
      val tamperedSignature = signature.substring(0, signature.length - 2) + "AB"

      crypto.verify(message, tamperedSignature) shouldBe false
    }

    "handle empty messages" in {
      val emptyMessage = ""
      val signature = crypto.sign(emptyMessage)

      signature should not be empty
      crypto.verify(emptyMessage, signature) shouldBe true
      crypto.verify("some-content", signature) shouldBe false
    }

    "handle special characters in messages" in {
      val specialMessage = "!@#$%^&*()_+{}[]|:;'<>,.?/~`"
      val signature = crypto.sign(specialMessage)

      crypto.verify(specialMessage, signature) shouldBe true
    }

    "handle unicode characters" in {
      val unicodeMessage = "こんにちは世界 👋 😊"
      val signature = crypto.sign(unicodeMessage)

      crypto.verify(unicodeMessage, signature) shouldBe true
    }

    "handle long messages" in {
      val longMessage = "a" * 10000
      val signature = crypto.sign(longMessage)

      crypto.verify(longMessage, signature) shouldBe true
    }
  }
}
