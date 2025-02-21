package utils

import play.api.Configuration

import javax.inject.{Inject, Singleton}


@Singleton
class OCGConfiguration @Inject()(config: Configuration) {

  object Redis {
    final val host          = config.getOptional[String]      ("redis.host")                   .getOrElse("localhost")
    final val port          = config.getOptional[Int]         ("redis.port")                   .getOrElse(6379)
    final val password      = config.getOptional[String]      ("redis.password")
    final val ttl           = config.getOptional[Int]         ("ttl")                          .getOrElse(36000)
  }

  object User {

    object Context {
      final val expiresAt   = config.getOptional[Long]       ("user.context.expiresAt")       .getOrElse(3600L)
    }

    final val authEnabled   = config.getOptional[Boolean]    ("user.authentication.enabled")  .getOrElse(true)
  }

  object Play {

    object Http {
      final val secretKey   = config.getOptional[String]      ("play.http.secret.key")        .getOrElse("")
    }
  }
}
