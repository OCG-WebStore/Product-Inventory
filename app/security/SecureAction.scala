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
                              parser: BodyParsers.Default,
                              crypto: Crypto
                            )(implicit ec: ExecutionContext)

  extends ActionBuilder[UserRequest, AnyContent] with Logging {

  override protected def executionContext: ExecutionContext = ec
  override def parser: BodyParser[AnyContent] = parser

  def adminAuth: ActionBuilder[UserRequest, AnyContent] = this.andThen(AdminFilter)
  def customerAuth: ActionBuilder[UserRequest, AnyContent] = this.andThen(CustomerFilter)


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

    UserContext(
      userId = params("user_id").toLongOption,
      roles = params("roles").split(",").map(Role.fromString).toSeq,
      expiresAt = config.User.Context.expiresAt
    )
  }

  def isValidTimestamp(userCtx: UserContext): Boolean =
    System.currentTimeMillis() - userCtx.timestamp < userCtx.expiresAt
}