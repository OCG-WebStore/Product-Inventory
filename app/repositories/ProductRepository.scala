package repositories

import models.Product

import scala.concurrent.Future

trait ProductRepository {
  def create(product: Product): Future[Product]
  def findById(id: Long): Future[Option[Product]]
  def findAll(): Future[Seq[Product]]
  def search(filter: ProductFilter): Future[Seq[Product]]
  def update(id: Long, product: Product): Future[Option[Product]]
  def delete(id: Long): Future[Boolean]
}