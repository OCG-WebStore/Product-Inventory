package services

import controllers.request.CreateProductRequest
import models.Product
import repositories.ProductRepository

import javax.inject.Inject
import scala.concurrent.Future

class ProductServiceImpl @Inject() (repository: ProductRepository) extends ProductService {

  override def createProduct(product: CreateProductRequest): Future[Product] =
    repository.create(CreateProductRequest.toProduct(product))

  override def getProduct(id: Long): Future[Option[Product]] =
    repository.findById(id)

  override def getAllProducts: Future[Seq[Product]] = repository.findAll()

  override def updateProduct(id: Long, product: Product): Future[Option[Product]] =
    repository.update(id, product)

  override def deleteProduct(id: Long): Future[Boolean] =
    repository.delete(id)
}
