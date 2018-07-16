import play.api._
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import com.softwaremill.macwire._
import router.Routes
import _root_.controllers.AssetsComponents

class EsApplicationLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    new EsComponents(context).application
  }
}

class EsComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with ControllersModule
    with AssetsComponents
    with HttpFiltersComponents {

  override lazy val router: routing.Router = {
    val prefix = "/"
    wire[Routes]
  }

}