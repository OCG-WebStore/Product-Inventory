package security

import utils.OCGConfiguration

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Singleton}
import javax.xml.bind.DatatypeConverter

@Singleton
class Crypto @Inject()(config: OCGConfiguration) {
  private val HMAC_SHA256 = "HmacSHA256"
  private val secret = config.Play.Http.secretKey

  def sign(message: String): String = {
    val mac = Mac.getInstance(HMAC_SHA256)
    mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), HMAC_SHA256))
    DatatypeConverter.printHexBinary(mac.doFinal(message.getBytes("UTF-8")))
  }

  def verify(message: String, signature: String): Boolean = {
    sign(message) == signature
  }
}