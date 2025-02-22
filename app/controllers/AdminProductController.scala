package controllers

import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import services.ProductService
import models.errors.{InvalidProduct, ProductNotFound}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import security.SecureActions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminProductController @Inject()(
                                   val controllerComponents: ControllerComponents,
                                   productService: ProductService,
                                   secureActions: SecureActions
                                 )(implicit ec: ExecutionContext)
  extends BaseController with Logging {

  def list(): Action[AnyContent] = secureActions.adminAuth.async { implicit request =>
    productService.getAllProducts.map { products =>
      Ok(Json.toJson(products))
    }
  }

  def get(id: Long): Action[AnyContent] = secureActions.adminAuth.async { implicit request =>
    productService.getProduct(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound(Json.toJson(ProductNotFound(id)))
    }
  }

  def create(): Action[CreateProductCommand] = secureActions.adminAuth.async(parse.json[CreateProductCommand]) { implicit request =>
    val createCommand = request.body

    productService.createProduct(createCommand).map { createdProduct =>
      Created(Json.toJson(createdProduct))
    }.recover {
      case _: Throwable => BadRequest(Json.toJson(InvalidProduct("Invalid product data")))
    }
  }

  def update(id: Long): Action[UpdateProductCommand] = secureActions.adminAuth.async(parse.json[UpdateProductCommand]) { implicit request =>
    val updateCommand = request.body

    productService.updateProduct(id, updateCommand).map {
      case Some(updatedProduct) => Ok(Json.toJson(updatedProduct))
      case None => NotFound(s"Product with id $id not found")
    }.recover {
      case _: Throwable => BadRequest(Json.toJson(InvalidProduct("Invalid product data")))
    }
  }

  def delete(id: Long): Action[AnyContent] = secureActions.adminAuth.async { implicit request =>
    productService.deleteProduct(id).map {
      case true => NoContent
      case false => NotFound(Json.toJson(ProductNotFound(id)))
    }
  }

  def ping(): Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok)
  }
}