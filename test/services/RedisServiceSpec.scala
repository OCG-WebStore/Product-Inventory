package services

import java.net.SocketException
import java.io.IOException
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import redis.{ByteStringSerializer, RedisClient}
import utils.OCGConfiguration
import models.{Category, Product}
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}


@RunWith(classOf[JUnitRunner])
class RedisServiceSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with MockitoSugar {
  
  private val configuration = new OCGConfiguration(Configuration(ConfigFactory.load))

  implicit val system: ActorSystem = ActorSystem("TestSystem")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = Materializer(system)

  val mockClient: RedisClient = mock[RedisClient]

  class TestRedisService(config: OCGConfiguration)(implicit ec: ExecutionContext, system: ActorSystem)
    extends RedisService(config, system) {
    override val client: RedisClient = mockClient
  }
  val service = new TestRedisService(configuration)

  val testProduct: Product = Product(
    id = Some(1L),
    name = "Test Product",
    description = "A test product",
    price = 100L,
    category = Category.Other,
    imageKey = "test.jpg"
  )

  "RedisService" should {

    "cache a product successfully" in {
      val key = s"product:${testProduct.id.get}"
      val jsonStr = Json.toJson(testProduct).toString

      when(mockClient.setex(key, configuration.Redis.ttl, jsonStr))
        .thenReturn(Future.successful(true))

      service.cacheProduct(testProduct).futureValue shouldBe true
    }

    "return false on cacheProduct when setex fails" in {
      val key = s"product:${testProduct.id.get}"
      val jsonStr = Json.toJson(testProduct).toString

      when(mockClient.setex(key, configuration.Redis.ttl, jsonStr))
        .thenReturn(Future.failed(new Exception("Test exception")))
      service.cacheProduct(testProduct).futureValue shouldBe false
    }

    "remove product cache successfully" in {
      val key = s"product:1"
      when(mockClient.del(key)).thenReturn(Future.successful(1L))
      service.removeProductCache(1L).futureValue shouldBe 1L
    }

    "return 0L on removeProductCache when deletion fails" in {
      val key = s"product:1"
      when(mockClient.del(key)).thenReturn(Future.failed(new Exception("Delete failed")))
      service.removeProductCache(1L).futureValue shouldBe 0L
    }

    "get cached product successfully" in {
      val key = s"product:1"
      val jsonStr = Json.toJson(testProduct).toString
      when(mockClient.get[String](key)).thenReturn(Future.successful(Some(jsonStr)))
      service.getCachedProduct(1L).futureValue shouldBe Some(testProduct)
    }

    "return None for getCachedProduct when key not found" in {
      val key = s"product:1"
      when(mockClient.get[String](key)).thenReturn(Future.successful(None))
      service.getCachedProduct(1L).futureValue shouldBe None
    }

    "return None for getCachedProduct when get fails" in {
      val key = s"product:1"
      when(mockClient.get[String](key)).thenReturn(Future.failed(new Exception("Get failed")))
      service.getCachedProduct(1L).futureValue shouldBe None
    }

    "cache category successfully" in {
      val key = s"products:category:${Category.Other.stringValue}"
      val ids = Seq(1L, 2L)
      val idsJson = Json.toJson(ids).toString
      when(mockClient.setex(key, configuration.Redis.ttl, idsJson)).thenReturn(Future.successful(true))
      service.cacheCategory(Category.Other, ids).futureValue shouldBe true
    }

    "get cached category ids successfully" in {
      val key = s"products:category:${Category.Other.stringValue}"
      val ids = Seq(1L, 2L)
      val idsJson = Json.toJson(ids).toString
      when(mockClient.get[String](key)).thenReturn(Future.successful(Some(idsJson)))
      service.getCachedCategoryIds("Other").futureValue shouldBe ids
    }

    "return empty sequence when getCachedCategoryIds finds nothing" in {
      val key = s"products:category:${Category.Other.stringValue}"
      when(mockClient.get[String](key)).thenReturn(Future.successful(None))
      service.getCachedCategoryIds("Other").futureValue shouldBe empty
    }

    "cache all product ids successfully" in {
      val key = "all_product_ids"
      val ids = Seq(1L, 2L, 3L)
      val idsJson = Json.toJson(ids).toString
      when(mockClient.setex(key, configuration.Redis.ttl, idsJson)).thenReturn(Future.successful(true))
      service.cacheAllProductsIds(ids).futureValue shouldBe true
    }

    "get cached product ids successfully" in {
      val key = "all_product_ids"
      val ids = Seq(1L, 2L, 3L)
      val idsJson = Json.toJson(ids).toString
      when(mockClient.get[String](key)).thenReturn(Future.successful(Some(idsJson)))
      service.getCachedProductIds.futureValue shouldBe ids
    }

    "return empty sequence for getCachedProductIds when get fails" in {
      val key = "all_product_ids"
      when(mockClient.get[String](key)).thenReturn(Future.failed(new Exception("Test exception")))
      service.getCachedProductIds.futureValue shouldBe empty
    }

    "recover from SocketException when getting key" in {
      val key = "product:1"
      when(mockClient.get[String](key))
        .thenReturn(Future.failed(new SocketException("Socket error")))
      service.getCachedProduct(1L).futureValue shouldBe None
    }

    "recover from IOException when setting key" in {
      when(mockClient.setex(any[String], any[Int], any[String])(any[ByteStringSerializer[String]]))
        .thenReturn(Future.failed(new IOException("IO Error")))
      service.cacheCategory(Category.Other, Seq.empty).futureValue shouldBe false
    }
  }
}
