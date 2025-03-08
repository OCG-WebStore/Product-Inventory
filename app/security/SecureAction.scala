package security

import play.api.Logging
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, BodyParsers, Request, Result, Results}
import security.filters.{AdminFilter, CustomerFilter}
import utils.OCGConfiguration

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


@Singleton
class SecureAction @Inject()(
                              config: OCGConfiguration,
                              bodyParser: BodyParsers.Default,
                              crypto: Crypto
                            )(implicit ec: ExecutionContext)

  extends ActionBuilder[UserRequest, AnyContent] with Logging {

  override protected def executionContext: ExecutionContext = ec
  override def parser: BodyParser[AnyContent] = bodyParser


  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    val contextHeader = request.headers.get("X-User-Context").getOrElse("")
    val signature = request.headers.get("X-Context-Signature").getOrElse("")
    val isVerifiedByCrypto = crypto.verify(crypto.sign(contextHeader), signature)
    val isUserAuthEnabled = config.User.authEnabled

    if(!isUserAuthEnabled){
      logger.debug(s"User authentication is disabled")
      block(UserRequest(
        UserContext(
          userId = None,
          roles = Seq(Role.ADMIN),
          expiresAt = 0L),
        request
      ))
    }
    else if(!isVerifiedByCrypto) {
      logger.debug(s"Failed authentication for User Context: $contextHeader")
      Future.successful(Results.Forbidden("Invalid context signature"))
    } else {
      Try(parseContext(contextHeader)) match {
        case Success(user) if isValidTimestamp(user) =>
          logger.debug(s"Authenticated User: Id = ${user.userId}")
          block(UserRequest(user, request))
        case Failure(_) =>
          logger.debug(s"Cannot parse Context Header: $contextHeader")
          Future.successful(Results.BadRequest("Invalid context format"))
        case _ =>
          logger.debug(s"Expired User Context: $contextHeader")
          Future.successful(Results.Forbidden("Expired context"))
      }
    }


  }

  private def parseContext(header: String): UserContext = {
    val params = header.split("&").map { pair =>
      val kv = pair.split("=")
      kv(0) -> kv(1)
    }.toMap

    val userId: Option[Long] = Try(params("user_id").toLongOption) match {
      case Success(value) => value match {
        case Some(v: Long) => Some(v)
        case _ => None
      }
      case Failure(_) => None
    }

    val roles = Try(params("roles").split(", ").map(Role.fromString).toSeq) match {
      case Success(value) => value match {
        case seq: Seq[Role] => seq
        case _ => Seq.empty[Role]
      }
      case Failure(_) => Seq.empty[Role]
    }

    val expiresAt = Try(params("expiresAt").toLongOption) match {
      case Success(value) => value match {
        case Some(v) => v
        case None => config.User.Context.expiresAt
      }
      case Failure(_) => config.User.Context.expiresAt
    }

    UserContext(
      userId = userId,
      roles = roles,
      expiresAt = expiresAt
    )
  }

  def isValidTimestamp(userCtx: UserContext): Boolean =
    System.currentTimeMillis() - userCtx.timestamp < userCtx.expiresAt
}

class SecureActions @Inject()(secureAction: SecureAction) {
  def adminAuth: ActionBuilder[UserRequest, AnyContent] = secureAction.andThen(AdminFilter)
  def customerAuth: ActionBuilder[UserRequest, AnyContent] = secureAction.andThen(CustomerFilter)
}