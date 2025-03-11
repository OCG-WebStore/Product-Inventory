package modules

import com.google.inject.AbstractModule
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import services.InventoryService
import utils.OCGConfiguration

import java.util.Properties
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

class InventoryModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[InventoryServiceLifecycle]).asEagerSingleton()
  }
}

@Singleton
class InventoryServiceLifecycle @Inject()(inventoryService: InventoryService,
                                          lifecycle: ApplicationLifecycle,
                                          config: OCGConfiguration
                                         )(implicit ec: ExecutionContext) extends Logging {

  private val adminProps = new Properties()
  adminProps.put("bootstrap.servers", config.Kafka.Bootstrap.servers)
  val adminClient: AdminClient = AdminClient.create(adminProps)

  val requiredTopics: Seq[NewTopic] = List(
    new NewTopic(config.Kafka.Topics.inventoryCommands, 6, 1.toShort),
    new NewTopic(config.Kafka.Topics.inventoryStates, 6, 1.toShort),
    new NewTopic(config.Kafka.Topics.inventoryEvents, 6, 1.toShort)
  )

  try {
    adminClient.createTopics(requiredTopics.asJava)
    logger.info("Created required Kafka topics")
  } catch {
    case e: Exception => logger.warn(s"Topic creation failed: ${e.getMessage}")
  } finally {
    adminClient.close()
  }

  inventoryService.start()

  lifecycle.addStopHook { () =>
    Future {
      inventoryService.stop()
    }
  }
}

