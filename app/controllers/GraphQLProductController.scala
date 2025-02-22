package controllers

import graphql.ProductSchema
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import sangria.marshalling.playJson._
import security.SecureActions

import services.ProductService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class GraphQLProductController @Inject()(
                                          cc: ControllerComponents,
                                          productService: ProductService,
                                          secureActions: SecureActions
                                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc){

  def products: Action[JsValue] = secureActions.customerAuth.async(parse.json) { request =>
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]
    val variables = (request.body \ "variables").toOption.flatMap {
      case JsString(vars) => Some(Json.parse(vars))
      case obj: JsObject => Some(obj)
      case _ => None
    }

    QueryParser.parse(query) match {
      case Success(value) =>
        Executor.execute(
          ProductSchema.schema,
          value,
          productService,
          operation,
          variables = variables.getOrElse(JsObject.empty),
          operationName = operation
          ).map(Ok(_))
        .recover{
          case error: QueryAnalysisError => BadRequest(error.resolveError)
          case error: ErrorWithResolver => InternalServerError(error.resolveError)
        }
      case Failure(exception) =>
        Future.successful(BadRequest(Json.obj("error" -> exception.getMessage)))
    }
  }

}
