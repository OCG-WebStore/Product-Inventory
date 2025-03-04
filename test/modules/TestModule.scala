package modules

import com.google.inject.AbstractModule
import play.api.{Application, Configuration, Environment}
import play.api.db.slick.{DatabaseConfigProvider, SlickApi}
import play.api.inject.guice.GuiceApplicationBuilder

class TestModule(env: Environment, config: Configuration) extends AbstractModule {
  override def configure(): Unit = {

    val testApp: Application = new GuiceApplicationBuilder()
      .configure(config)
      .build()

    val dbConfigProvider = testApp.injector.instanceOf[DatabaseConfigProvider]

    bind(classOf[Application]).toInstance(testApp)
    bind(classOf[DatabaseConfigProvider]).toInstance(dbConfigProvider)
    bind(classOf[SlickApi]).toInstance(testApp.injector.instanceOf[SlickApi])
  }
}