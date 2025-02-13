package modules

import com.google.inject.AbstractModule
import repositories.{ProductRepository, ProductRepositoryImpl}
import services.{ProductService, ProductServiceImpl}

class ProductModule extends AbstractModule {
  override def configure(): Unit = {

    bind(classOf[ProductRepository])
      .to(classOf[ProductRepositoryImpl])
      .asEagerSingleton()

    bind(classOf[ProductService])
      .to(classOf[ProductServiceImpl])
      .asEagerSingleton()

  }
}