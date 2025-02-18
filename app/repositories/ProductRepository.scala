package repositories

import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.Product

import scala.concurrent.Future

trait ProductRepository {
  def create(command: CreateProductCommand): Future[Product]
  def findById(id: Long): Future[Option[Product]]
  def findByCategory(category: String): Future[Seq[Product]]
  def findAll(): Future[Seq[Product]]
  def update(id: Long, command: UpdateProductCommand): Future[Option[Product]]
  def delete(id: Long): Future[Boolean]
}