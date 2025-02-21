package security

import play.api.mvc.{Request, WrappedRequest}

case class UserRequest[A](
                      user: UserContext,
                      request: Request[A]
                    ) extends WrappedRequest[A](request)