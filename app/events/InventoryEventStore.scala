package events

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import models.Inventory
import models.errors.EventPublishingError
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import play.api.Logging
import play.api.libs.json.Json
import utils.OCGConfiguration

import java.net.InetAddress
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import javax.inject._
import scala.concurrent.duration.DurationInt


@Singleton
class InventoryEventStore @Inject()(config: OCGConfiguration)
                         (implicit
                          system: ActorSystem,
                          mat: Materializer,
                          ec: ExecutionContext
                         ) extends Logging {

  private val bootstrapServers = config.Kafka.Bootstrap.servers
  private val inventoryEvents = config.Kafka.Topics.inventoryEvents

  private val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(bootstrapServers)

  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)

  private def serializeEvent(event: InventoryEvent): String = {
    event match {
      case e: StockAdded => Json.toJson(e).toString()
      case e: StockReserved => Json.toJson(e).toString()
      case e: ReservationConfirmed => Json.toJson(e).toString()
      case e: ReservationCancelled => Json.toJson(e).toString()
    }
  }

  private def deserializeEvent(json: String, eventType: String): Option[InventoryEvent] = {
    eventType match {
      case "StockAdded" => Json.parse(json).asOpt[StockAdded]
      case "StockReserved" => Json.parse(json).asOpt[StockReserved]
      case "ReservationConfirmed" => Json.parse(json).asOpt[ReservationConfirmed]
      case "ReservationCancelled" => Json.parse(json).asOpt[ReservationCancelled]
      case _ => None
    }
  }

  def publish(event: InventoryEvent): Future[Unit] = {
    val eventType = event.getClass.getSimpleName
    val key = event.productId
    val value = serializeEvent(event)
    val record = new ProducerRecord(inventoryEvents, key, s"$eventType:$value")

    Source.single(record)
      .runWith(Producer.plainSink(producerSettings))
      .map(_ => ())
      .recover { case ex =>
        logger.info(s"Failed to publish event $eventType for product ${event.productId}: ${ex.getMessage}")
        throw EventPublishingError(s"Failed to publish event: ${ex.getMessage}", Some(ex))
      }
  }

  def getEvents(productId: String): Future[Seq[InventoryEvent]] = {
    val hostName = InetAddress.getLocalHost.getHostName
    val groupId = s"inventory-event-store-$hostName-${UUID.randomUUID()}"
    Consumer
      .plainSource(
        consumerSettings
          .withGroupId(groupId)
          .withProperties(
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> "false"
      ),
        Subscriptions.topics(inventoryEvents)
      )
      .filter(record => record.key() == productId)
      .map { record =>
        val parts = record.value().split(":", 2)
        val eventType = parts(0)
        val json = parts(1)
        deserializeEvent(json, eventType)
      }
      .collect { case Some(event) => event }
      .takeWithin(5.seconds)
      .runWith(Sink.seq)
      .recover { case ex =>
        logger.debug(s"Failed to retrieve events for product $productId: ${ex.getMessage}")
        Seq.empty[InventoryEvent] }
  }

  def getCurrentState(productId: String): Future[Option[Inventory]] = {
    getEvents(productId).map {
      case events: Seq[InventoryEvent] if events.nonEmpty => InventoryEvents.rebuild(events)
      case _ => None
    }
  }
}
