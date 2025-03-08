package security

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, OK}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, ControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import utils.OCGConfiguration

import scala.concurrent.Future


@RunWith(classOf[JUnitRunner])
class SecureActionSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with MockitoSugar {

  implicit val cc: ControllerComponents = Helpers.stubControllerComponents()
  implicit val system: ActorSystem = ActorSystem("TestSystem")
  implicit val mat: Materializer = Materializer(system)

  val conf: Configuration = Configuration(ConfigFactory.load)
  val ocgConfig: OCGConfiguration = new OCGConfiguration(Configuration(
    conf.underlying.withValue("user.authentication.enabled", ConfigValueFactory.fromAnyRef(true))
  ))
  val crypto: Crypto = new Crypto(ocgConfig) {
    override def verify(message: String, signature: String): Boolean = true
  }

  val parser: BodyParser[AnyContent] = cc.parsers.anyContent
  val secureAction = new SecureAction(ocgConfig, parser.asInstanceOf[BodyParsers.Default], crypto)(cc.executionContext)
  
  def futureOk: Future[Result] = Future.successful(Ok("Success"))


  "SecureAction" should {
    "allow requests when user authentication is disabled" in {
      val noAuthConfig: OCGConfiguration = new OCGConfiguration(Configuration(
        conf.underlying.withValue("user.authentication.enabled", ConfigValueFactory.fromAnyRef(false))
      ))
      val noAuthCrypto = new Crypto(noAuthConfig) {
        override def verify(message: String, signature: String): Boolean = true
      }
      val noAuthParser: BodyParser[AnyContent] = cc.parsers.anyContent
      val noAuthSecureAction = new SecureAction(noAuthConfig, noAuthParser.asInstanceOf[BodyParsers.Default], noAuthCrypto)(cc.executionContext)

      val request = FakeRequest().withHeaders("X-User-Context" -> "some-context")
      val result = noAuthSecureAction.invokeBlock(
        request,
        (_: UserRequest[AnyContent]) => futureOk)

      status(result) shouldBe OK
    }

    "reject requests with invalid context signature" in {
      val request = FakeRequest().withHeaders("X-User-Context" -> "some-context", "X-Context-Signature" -> "invalid-signature")
      val result = secureAction.invokeBlock(
        request, 
        (_: UserRequest[AnyContent]) => futureOk)

      status(result) shouldBe BAD_REQUEST
    }

    "authenticate user with valid context" in {
      val request = FakeRequest().withHeaders("X-User-Context" -> "user_id=123&roles=ADMIN", "X-Context-Signature" -> "valid-signature")
      val result = secureAction.invokeBlock(request, (userRequest: UserRequest[AnyContent]) => {
        userRequest.user.userId mustBe Some(123)
        Future.successful(Ok(s"User ID: ${userRequest.user.userId}"))
      })

      val content = result.futureValue.body.dataStream.runFold("")(_ + _.utf8String).futureValue
      content shouldBe "User ID: Some(123)"
      status(result) shouldBe OK
    }

    "reject expired user context" in {
      val request = FakeRequest().withHeaders("X-User-Context" -> "user_id=123&roles=ADMIN&expiresAt=-1000", "X-Context-Signature" -> "valid-signature")
      val result = secureAction.invokeBlock(
        request,
        (_: UserRequest[AnyContent]) => futureOk)

      status(result) shouldBe FORBIDDEN
    }
  }

  val secureActions = new SecureActions(secureAction)

  "SecureActions" should {
    "apply admin filter and accept admin user" in {
      val secureActions = new SecureActions(secureAction)

      val request = FakeRequest().withHeaders("X-User-Context" -> "user_id=123&roles=ADMIN", "X-Context-Signature" -> "valid-signature")
      val result = secureActions.adminAuth.invokeBlock(
        request,
        (userRequest: UserRequest[AnyContent]) => {
          userRequest.user.roles.head mustBe Role.ADMIN
          Future.successful(Ok(s"Role: ${userRequest.user.roles.head}"))
        }
      )

      status(result) shouldBe OK
    }

    "apply admin filter and reject non-admin user" in {
      val request = FakeRequest().withHeaders("X-User-Context" -> "user_id=123&roles=CUSTOMER", "X-Context-Signature" -> "valid-signature")
      val result = secureActions.adminAuth.invokeBlock(
        request,
        (userRequest: UserRequest[AnyContent]) => {
          Future.successful(Ok(s"Role: ${userRequest.user.roles.head}"))
        }
      )

      status(result) shouldBe FORBIDDEN
    }

    "apply customer filter and accept customer user" in {
      val request = FakeRequest().withHeaders("X-User-Context" -> "user_id=123&roles=CUSTOMER", "X-Context-Signature" -> "valid-signature")
      val result = secureActions.customerAuth.invokeBlock(
        request,
        (userRequest: UserRequest[AnyContent]) => {
          userRequest.user.roles.head mustBe Role.CUSTOMER
          Future.successful(Ok(s"Role: ${userRequest.user.roles.head}"))
        }
      )

      status(result) shouldBe OK
    }

    "apply customer filter and reject non-customer user" in {
      val request = FakeRequest().withHeaders("X-User-Context" -> "user_id=123", "X-Context-Signature" -> "valid-signature")
      val result = secureActions.customerAuth.invokeBlock(
        request,
        (userRequest: UserRequest[AnyContent]) => {
          Future.successful(Ok(s"Role: ${userRequest.user.roles.head}"))
        }
      )

      status(result) shouldBe FORBIDDEN
    }
  }
}
