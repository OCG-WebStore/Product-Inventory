package utils

import play.api.Configuration

import javax.inject.{Inject, Singleton}


@Singleton
class OCGConfiguration @Inject()(config: Configuration) {

  object User {

    object Context {
      final val expiresAt         = config.getOptional[Long]        ("user.context.expiresAt")          .getOrElse(3600L)
    }

    final val authEnabled         = config.getOptional[Boolean]     ("user.authentication.enabled")     .getOrElse(true)
  }

  object Play {

    object Http {
      final val secretKey         = config.getOptional[String]      ("play.http.secret.key")            .getOrElse("")
    }
  }

  object Redis {

    final val host                = config.getOptional[String]      ("redis.host")                      .getOrElse("localhost")
    final val port                = config.getOptional[Int]         ("redis.port")                      .getOrElse(6379)
    final val password            = config.getOptional[String]      ("redis.password")
    final val ttl                 = config.getOptional[Int]         ("ttl")                             .getOrElse(36000)
  }

  object Kafka {

    object Bootstrap {
      final val servers           = config.getOptional[String]      ("kafka.bootstrap.servers")         .getOrElse("localhost:9092")
    }

    object Topics {
      final val inventoryEvents   = config.getOptional[String]      ("kafka.topics.inventoryEvents")    .getOrElse("inventory-events")
      final val inventoryStates   = config.getOptional[String]      ("kafka.topics.inventoryStates")    .getOrElse("inventory-states")
      final val inventoryCommands = config.getOptional[String]      ("kafka.topics.inventoryCommands")  .getOrElse("inventory-commands")
    }
  }
}
