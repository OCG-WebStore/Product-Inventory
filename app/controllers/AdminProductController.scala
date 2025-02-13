package controllers

import controllers.request.CreateProductRequest
import models.Product
import services.ProductService
import models.errors.{InvalidProduct, ProductNotFound}
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminProductController @Inject()(
                                   val controllerComponents: ControllerComponents,
                                   productService: ProductService
                                 )(implicit ec: ExecutionContext) extends BaseController {

  def list(): Action[AnyContent] = Action.async { implicit request =>
    productService.getAllProducts.map { products =>
      Ok(Json.toJson(products))
    }
  }

  def get(id: Long): Action[AnyContent] = Action.async { implicit request =>
    productService.getProduct(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound(Json.toJson(ProductNotFound(id)))
    }
  }

  def create(): Action[CreateProductRequest] = Action.async(parse.json[CreateProductRequest]) { implicit request =>
    val productRequest = request.body
    val product = Product(None, productRequest.name, productRequest.description, productRequest.price, productRequest.category, productRequest.imageUrl)

    productService.createProduct(productRequest).map { createdProduct =>
      Created(Json.toJson(createdProduct))
    }.recover {
      case _: Throwable => BadRequest(Json.toJson(InvalidProduct("Invalid product data")))
    }
  }

  def update(id: Long): Action[CreateProductRequest] = Action.async(parse.json[CreateProductRequest]) { implicit request =>
    val productRequest = request.body
    val product = Product(Some(id), productRequest.name, productRequest.description, productRequest.price, productRequest.category, productRequest.imageUrl)

    productService.updateProduct(id, product).map {
      case Some(updatedProduct) => Ok(Json.toJson(updatedProduct))
      case None => NotFound(s"Product with id $id not found")
    }.recover {
      case _: Throwable => BadRequest(Json.toJson(InvalidProduct("Invalid product data")))
    }
  }

  def delete(id: Long): Action[AnyContent] = Action.async { implicit request =>
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