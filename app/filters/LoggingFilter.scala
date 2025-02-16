package filters

import play.api.mvc._
import play.api.Logger
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class LoggingFilter @Inject() (implicit ec: ExecutionContext) extends EssentialFilter {
  private val logger = Logger("AdminProductController")

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { request =>
    val startTime = System.currentTimeMillis()

    next(request).map { result =>
      val endTime = System.currentTimeMillis()
      val requestTime = endTime - startTime

      logger.info(
        s"${request.method} | " +
          s"${request.host}${request.uri} | " +
          s"Remote-Address: ${request.remoteAddress} | " +
          s"Status: ${result.header.status} | " +
          s"Time: ${requestTime}ms"
      )
      result
    }
  }
}