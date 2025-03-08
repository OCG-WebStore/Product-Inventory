package security

import play.api.mvc._
import utils.OCGConfiguration

import scala.concurrent.{ExecutionContext, Future}

class TestSecureAction(
                        config: OCGConfiguration,
                        bodyParser: BodyParsers.Default,
                        crypto: Crypto,
                        cc: ControllerComponents
                      )(implicit val ec: ExecutionContext)
        extends SecureAction(config, bodyParser, crypto) {

  override def parser: BodyParser[AnyContent] = cc.parsers.anyContent

  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    val dummyUserContext = UserContext(
            userId = Some(1L),
            roles = Seq(Role.ADMIN),
            expiresAt = 8000L
    )
    block(UserRequest(dummyUserContext, request))
  }

  override protected def executionContext: ExecutionContext = ec
}

class TestSecureActions(
                         config: OCGConfiguration,
                         bodyParser: BodyParsers.Default,
                         crypto: Crypto,
                         cc: ControllerComponents
)(implicit val ec: ExecutionContext)
  extends SecureActions(new TestSecureAction(config, bodyParser, crypto, cc)(ec)) {
    override def adminAuth: ActionBuilder[UserRequest, AnyContent] = new TestSecureAction(config,bodyParser, crypto, cc)(ec)
}