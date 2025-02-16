package services

import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.Product

import scala.concurrent.Future

trait ProductService {
  def createProduct(product: CreateProductCommand): Future[Product]
  def getProduct(id: Long): Future[Option[Product]]
  def getAllProducts: Future[Seq[Product]]
  def updateProduct(id: Long, product: UpdateProductCommand): Future[Option[Product]]
  def deleteProduct(id: Long): Future[Boolean]
}