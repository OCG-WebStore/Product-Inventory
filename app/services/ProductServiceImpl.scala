package services

import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.Product
import repositories.ProductRepository

import javax.inject.Inject
import scala.concurrent.Future

class ProductServiceImpl @Inject() (repository: ProductRepository) extends ProductService {

  override def createProduct(productCommand: CreateProductCommand): Future[Product] =
    repository.create(productCommand)

  override def getProduct(id: Long): Future[Option[Product]] =
    repository.findById(id)

  override def getAllProducts: Future[Seq[Product]] = repository.findAll()

  override def updateProduct(id: Long, productCommand: UpdateProductCommand): Future[Option[Product]] =
    repository.update(id, productCommand)

  override def deleteProduct(id: Long): Future[Boolean] =
    repository.delete(id)
}
