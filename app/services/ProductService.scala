package services

import controllers.request.CreateProductRequest
import models.Product

import scala.concurrent.Future

trait ProductService {
  def createProduct(product: CreateProductRequest): Future[Product]
  def getProduct(id: Long): Future[Option[Product]]
  def getAllProducts: Future[Seq[Product]]
  def updateProduct(id: Long, product: Product): Future[Option[Product]]
  def deleteProduct(id: Long): Future[Boolean]
}