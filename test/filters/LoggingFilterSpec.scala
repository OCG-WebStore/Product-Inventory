package filters

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import org.junit.runner.RunWith
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import play.api.http.Status
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers.{be, convertToAnyMustWrapper, include, not}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.slf4j
import play.api.libs.streams.Accumulator
import play.api.test.Helpers._


@RunWith(classOf[JUnitRunner])
class LoggingFilterSpec extends AnyWordSpec with MockitoSugar {

  implicit val system: ActorSystem = ActorSystem("TestSystem")
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  "LoggingFilter" should {

    "log request details with correct format" in {
      val underlyingLogger: slf4j.Logger = mock[org.slf4j.Logger]
      val testLogger: Logger = new Logger(underlyingLogger)
      val filter: LoggingFilter = new LoggingFilter()(ec) {
        override val logger: Logger = testLogger
      }

      when(underlyingLogger.isInfoEnabled()).thenReturn(true)

      val nextAction = new EssentialAction {
        override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
          Accumulator.done(Future.successful(Results.Ok("Success")))
        }
      }
      val fakeRequest = FakeRequest("GET", "/test-path")
        .withHeaders("Host" -> "test.example.com")
      val result = filter.apply(nextAction)(fakeRequest)
      val resultValue = await(result.run())(5 seconds)

      resultValue.header.status mustBe Status.OK

      val infoCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(underlyingLogger, times(1)).info(infoCaptor.capture())

      val logMessage = infoCaptor.getValue
      logMessage must include("GET |")
      logMessage must include("test.example.com/test-path |")
      logMessage must include("Remote-Address: 127.0.0.1 |")
      logMessage must include("Status: 200 |")
      logMessage must include("Time:")
      logMessage must include("ms")
    }

    "correctly measure and log request processing time" in {
      val underlyingLogger: slf4j.Logger = mock[org.slf4j.Logger]
      val testLogger: Logger = new Logger(underlyingLogger)
      val filter: LoggingFilter = new LoggingFilter()(ec) {
        override val logger: Logger = testLogger
      }

      when(underlyingLogger.isInfoEnabled()).thenReturn(true)

      val nextAction = new EssentialAction {
        override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
          Accumulator.done(Future {
            Thread.sleep(100)
            Results.Ok("Delayed response")
          }(ec))
        }
      }

      val fakeRequest = FakeRequest("POST", "/api/data")
        .withHeaders("Host" -> "api.example.com")
      val result = filter.apply(nextAction)(fakeRequest)
      val resultValue = await(result.run())(5 seconds)

      resultValue.header.status mustBe Status.OK

      val infoCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(underlyingLogger, times(1)).info(infoCaptor.capture())

      val logMessage = infoCaptor.getValue
      logMessage must include("POST |")

      val timePattern = "Time: (\\d+)ms".r
      val timeMatch = timePattern.findFirstMatchIn(logMessage)
      timeMatch must not be None

      val processingTime = timeMatch.get.group(1).toInt
      processingTime must be >= 100
    }

    "handle different HTTP methods and status codes correctly" in {
      val testCases = Seq(
        ("GET", "/users", Status.OK),
        ("POST", "/users", Status.CREATED),
        ("PUT", "/users/1", Status.NO_CONTENT),
        ("DELETE", "/users/1", Status.OK),
        ("PATCH", "/users/1", Status.OK),
        ("HEAD", "/health", Status.OK),
        ("OPTIONS", "/api", Status.OK),
        ("GET", "/not-found", Status.NOT_FOUND),
        ("POST", "/unauthorized", Status.UNAUTHORIZED)
      )

      for ((method, path, status) <- testCases) {
        val underlyingLogger: slf4j.Logger = mock[org.slf4j.Logger]
        val testLogger: Logger = new Logger(underlyingLogger)
        val filter: LoggingFilter = new LoggingFilter()(ec) {
          override val logger: Logger = testLogger
        }

        when(underlyingLogger.isInfoEnabled()).thenReturn(true)

        val nextAction = new EssentialAction {
          override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
            Accumulator.done(Future.successful(Results.Status(status)))
          }
        }

        val fakeRequest = FakeRequest(method, path)
          .withHeaders("Host" -> "test.example.com")

        val result = filter.apply(nextAction)(fakeRequest)
        val resultValue = await(result.run())(5 seconds)

        resultValue.header.status mustBe status

        val infoCaptor = ArgumentCaptor.forClass(classOf[String])
        verify(underlyingLogger, times(1)).info(infoCaptor.capture())

        val logMessage = infoCaptor.getValue
        logMessage must include(s"$method |")
        logMessage must include(s"test.example.com$path |")
        logMessage must include(s"Status: $status |")
      }
    }

    "preserve the original result without modification" in {
      val underlyingLogger: slf4j.Logger = mock[org.slf4j.Logger]
      val testLogger: Logger = new Logger(underlyingLogger)
      val filter: LoggingFilter = new LoggingFilter()(ec) {
        override val logger: Logger = testLogger
      }

      when(underlyingLogger.isInfoEnabled()).thenReturn(true)

      val customResult = Results.Ok("Custom body")
        .withHeaders("X-Custom-Header" -> "custom-value")
        .withCookies(Cookie("test-cookie", "cookie-value"))

      val nextAction = new EssentialAction {
        override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
          Accumulator.done(Future.successful(customResult))
        }
      }

      val fakeRequest = FakeRequest("GET", "/test-path")

      val result = filter.apply(nextAction)(fakeRequest)
      val resultValue = await(result.run())(5 seconds)

      resultValue.header.status mustBe Status.OK
      contentAsString(Future.successful(resultValue)) mustBe "Custom body"
      resultValue.header.headers.get("X-Custom-Header") mustBe Some("custom-value")
      cookies(Future.successful(resultValue)).get("test-cookie").map(_.value) mustBe Some("cookie-value")
    }
  }
}