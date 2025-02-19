package services

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import akka.actor.ActorSystem
import models.{Category, Product}

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import play.api.libs.json.{Json, Reads, Writes}
import redis.RedisClient

import java.io.IOException
import java.net.SocketException

@Singleton
class RedisService @Inject() (
                               config: Configuration,
                               implicit val system: ActorSystem
                             )(implicit ec: ExecutionContext) extends Logging {

  val client: RedisClient = RedisClient(
    host      = config.getOptional[String]  ("redis.host").getOrElse("localhost"),
    port      = config.getOptional[Int]     ("redis.port").getOrElse(6379),
    password  = Option(config.get[String]   ("redis.password"))
  )

  val ttl: Int = config.getOptional[Int]("ttl").getOrElse(36000)

  private def productKey(id: Long): String              = s"product:$id"
  private def categoryKey(category: Category): String   = s"products:category:${category.stringValue}"
  private val allProductIdsKey                          = "all_product_ids"

  def cacheProduct(product: Product): Future[Boolean] = {
    val key = productKey(product.id.get)
    set(key, Json.toJson(product).toString)
  }

  def removeProductCache(id: Long): Future[Long] = delete(productKey(id))

  def getCachedProduct(id: Long): Future[Option[Product]] = {
    get(productKey(id)).map { jsonStrOpt =>
      jsonStrOpt.flatMap { jsonStr =>
        Json.parse(jsonStr).asOpt[Product]
      }
    }
  }

  def cacheCategory(category: Category, ids: Seq[Long]): Future[Boolean] = {
    val key = categoryKey(category)
    set(key, Json.toJson(ids).toString)
  }
  def getCachedCategoryIds(category: String): Future[Seq[Long]] = {
    get(categoryKey(Category.fromString(category))).map { idsStrOpt =>
      idsStrOpt
        .map(str => Json.parse(str).as[Seq[Long]])
        .getOrElse(Seq.empty[Long])
    }
  }

  def cacheAllProductsIds(ids: Seq[Long]): Future[Boolean] = setList[Long](allProductIdsKey, ids)

  def getCachedProductIds: Future[Seq[Long]] = getList[Long](allProductIdsKey)

  private def get(key: String): Future[Option[String]] = {
    client.get[String](key).recover(handleExceptions(key).andThen(_ => None))
  }

  private def set(key: String, value: String): Future[Boolean] = {
    client.setex(key, ttl, value).recover(handleExceptions(key).andThen(_ => false))
  }

  private def delete(key: String): Future[Long] = {
    client.del(key).recover(handleExceptions(key))
  }

  private def getList[T](key: String)(implicit reads: Reads[T]): Future[Seq[T]] = {
    client.get[String](key)
      .map {
        case Some(jsonStr) => Json.parse(jsonStr).as[Seq[T]]
        case None => Seq.empty[T]
      }
      .recover(handleExceptions(key).andThen(_ => Seq.empty[T]))
  }

  private def setList[T](key: String, values: Seq[T])(implicit writes: Writes[T]): Future[Boolean] = {
    val jsonStr = Json.toJson(values).toString
    client.setex(key, ttl, jsonStr).recover(handleExceptions(key).andThen(_ => false))
  }

  private def handleExceptions(key: String): PartialFunction[Throwable, Long] = {
    case ex: SocketException =>
      logger.error(s"Redis connection error at $key: ${ex.getMessage}", ex)
      0L
    case ex: IOException =>
      logger.error(s"Redis IO error at key $key: ${ex.getMessage}", ex)
      0L
    case ex: Exception =>
      logger.error(s"Unexpected redis error at key $key: ${ex.getMessage}", ex)
      0L
  }
}
