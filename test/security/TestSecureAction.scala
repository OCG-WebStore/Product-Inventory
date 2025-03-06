package security;

import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

class TestSecureAction(implicit val ec: ExecutionContext, cc: ControllerComponents)
        extends ActionBuilder[UserRequest, AnyContent] {

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

class TestSecureActions(implicit ec: ExecutionContext, cc: ControllerComponents)
  extends SecureActions(new TestSecureAction()(ec, cc)) {
    override def adminAuth: ActionBuilder[UserRequest, AnyContent] = new TestSecureAction()(ec, cc)
}