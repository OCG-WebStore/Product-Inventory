package graphql

import sangria.ast.{BigIntValue, IntValue, StringValue}
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

import java.time.Instant
import scala.util.Try

object CustomScalars {

  case object InstantCoercionViolation extends ValueCoercionViolation("Invalid ISO-8601 datetime format")
  case object LongCoercionViolation extends ValueCoercionViolation("Long value expected")

  object Implicits {

    implicit val InstantType: ScalarType[Instant] = ScalarType[Instant](
      "Instant",
      coerceOutput = (instant, _) => instant.toString,
      coerceInput = {
        case StringValue(value, _, _, _, _) => Try(Instant.parse(value))
          .toEither.left.map(_ => InstantCoercionViolation)
        case _ => Left(InstantCoercionViolation)
      },
      coerceUserInput = {
        case s: String => Try(Instant.parse(s))
          .toEither.left.map(_ => InstantCoercionViolation)
        case _ => Left(InstantCoercionViolation)
      }
    )

    implicit val LongType: ScalarType[Long] = ScalarType[Long](
      "Long",
      coerceOutput = (value, _) => value,
      coerceInput = {
        case IntValue(i, _, _) => Right(i.toLong)
        case BigIntValue(l, _, _) => Right(l.toLong)
        case StringValue(s, _, _, _, _) => Try(s.toLong)
          .toEither.left.map(_ => LongCoercionViolation)
        case _ => Left(LongCoercionViolation)
      },
      coerceUserInput = {
        case i: Int => Right(i.toLong)
        case l: Long => Right(l)
        case s: String => Try(s.toLong)
          .toEither.left.map(_ => LongCoercionViolation)
        case _ => Left(LongCoercionViolation)
      }
    )
  }
}
