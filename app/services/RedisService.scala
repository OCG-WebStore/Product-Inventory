package services

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import redis.RedisClient
import akka.actor.ActorSystem

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import play.api.libs.json.{Json, Reads, Writes}

import java.io.IOException
import java.net.SocketException

@Singleton
class RedisService @Inject() (
                               config: Configuration,
                               implicit val system: ActorSystem
                             )(implicit ec: ExecutionContext) extends Logging {

  val client: RedisClient = RedisClient(
    host = config.getOptional[String]("redis.host").getOrElse("localhost"),
    port = config.getOptional[Int]("redis.port").getOrElse(6379),
    password = Some(config.get[String]("redis.password"))
  )

  val ttl: Int = config.getOptional[Int]("ttl").getOrElse(36000)

  private val allProductIdsKey = "all_product_ids"

  def getAllProductIds: Future[Seq[Long]] = {
    getList[Long](allProductIdsKey)
  }

  def setAllProductIds(ids: Seq[Long]): Future[Boolean] = {
    setList[Long](allProductIdsKey, ids)
  }

  def get(key: String): Future[Option[String]] = {
    client.get[String](key).recover(handleExceptions(key).andThen(_ => None))
  }

  def set(key: String, value: String): Future[Boolean] = {
    client.setex(key, ttl, value).recover(handleExceptions(key).andThen(_ => false))
  }

  def delete(key: String): Future[Long] = {
    client.del(key).recover(handleExceptions(key))
  }

  def getList[T](key: String)(implicit reads: Reads[T]): Future[Seq[T]] = {
    client.get[String](key)
      .map {
        case Some(jsonStr) => Json.parse(jsonStr).as[Seq[T]]
        case None => Seq.empty[T]
      }
      .recover(handleExceptions(key).andThen(_ => Seq.empty[T]))
  }

  def setList[T](key: String, values: Seq[T])(implicit writes: Writes[T]): Future[Boolean] = {
    val jsonStr = Json.toJson(values).toString
    client.setex(key, ttl, jsonStr).recover(handleExceptions(key).andThen(_ => false))
  }

  def handleExceptions(key: String): PartialFunction[Throwable, Long] = {
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
