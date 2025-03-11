package services

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import models.Inventory
import models.commands.{AddStock, CancelReservation, ConfirmReservation, InventoryCommandHandler, ReserveStock}
import models.errors.CommandProcessingError
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import play.api.Logging
import play.api.libs.json._
import utils.OCGConfiguration

import java.util.UUID
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InventoryService @Inject()(
                                  commandHandler: InventoryCommandHandler,
                                  config: OCGConfiguration)
                                (implicit
                                 system: ActorSystem,
                                 mat: Materializer,
                                 ec: ExecutionContext
                                ) extends Logging {

  private val bootstrapServers = config.Kafka.Bootstrap.servers
  private val commandsTopic = config.Kafka.Topics.inventoryCommands
  private val statesTopic = config.Kafka.Topics.inventoryStates

  private val producerSettings = ProducerSettings(system,
    new StringSerializer,
    new StringSerializer)
    .withBootstrapServers(bootstrapServers)


  private var consumerControl: Option[Consumer.Control] = None

  def start(): Unit = {
    val consumerSettings = ConsumerSettings(system,
      new StringDeserializer,
      new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId("inventory-service-main")
      .withClientId(s"inventory-service-main-${UUID.randomUUID()}")

    consumerControl = Some(
      Consumer
        .plainSource(consumerSettings, Subscriptions.topics(commandsTopic))
        .mapAsync(4)(processCommand)
        .toMat(Sink.ignore)(Keep.left)
        .run()
    )

    logger.info("Inventory service started")
  }

  def stop(): Unit = {
    consumerControl.map(_.shutdown()).getOrElse(Future.successful(()))
    logger.debug("Inventory service stopped")
  }

  private def processCommand(record: ConsumerRecord[String, String]): Future[Unit] = {
    val commandType = record.key()
    val commandJson = record.value()

    logger.info("Processing command: " + record.key() + " " + record.value())

    val commandResult = commandType match {
      case "AddStock" =>
        Json.parse(commandJson).validate[AddStock].asOpt
          .map(commandHandler.handle)
          .getOrElse(Future.successful(Left(CommandProcessingError("Invalid command format"))))

      case "ReserveStock" =>
        Json.parse(commandJson).validate[ReserveStock].asOpt
          .map(commandHandler.handle)
          .getOrElse(Future.successful(Left(CommandProcessingError("Invalid command format"))))

      case "ConfirmReservation" =>
        Json.parse(commandJson).validate[ConfirmReservation].asOpt
          .map(commandHandler.handle)
          .getOrElse(Future.successful(Left(CommandProcessingError("Invalid command format"))))

      case "CancelReservation" =>
        Json.parse(commandJson).validate[CancelReservation].asOpt
          .map(commandHandler.handle)
          .getOrElse(Future.successful(Left(CommandProcessingError("Invalid command format"))))

      case _ =>
        Future.successful(Left(CommandProcessingError(s"Unknown command type: $commandType")))
    }

    commandResult.flatMap {
      case Right(inventoryItem) => publishStateUpdate(inventoryItem)
      case Left(error) =>
        logger.error(s"Error processing command $commandType: $error")
        Future.successful(())
    }
  }

  private def publishStateUpdate(item: Inventory): Future[Unit] = {
    val record = new ProducerRecord(
      statesTopic,
      item.productId,
      Json.toJson(item).toString()
    )

    Source.single(record)
      .runWith(Producer.plainSink(producerSettings))
      .map(_ => ())
  }
}
