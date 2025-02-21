package security.filters

import play.api.Logging
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionFilter, Result}
import security.{Role, UserRequest}

import scala.concurrent.{ExecutionContext, Future}

trait RoleFilter extends ActionFilter[UserRequest] with Logging {

  def role: Role

  import scala.concurrent.ExecutionContext.global
  override protected def executionContext: ExecutionContext = global

  override protected def filter[A](request: UserRequest[A]): Future[Option[Result]] =
    Future.successful {
      if(!(request.user.hasRole(role) || request.user.isAdmin)) {
        logger.debug(s"User: Id = ${request.user.userId} does not have required $role access");
        Some(Forbidden(s"$role access required."))
      }
      else None
    }
}

object AdminFilter extends RoleFilter {
  override def role: Role = Role.ADMIN
}

object CustomerFilter extends RoleFilter {
  override def role: Role = Role.CUSTOMER
}
