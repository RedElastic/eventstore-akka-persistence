import com.softwaremill.macwire._
import controllers.{HomeController, UserController}
import play.api.i18n.Langs
import play.api.mvc.ControllerComponents
import services.ServicesModule

trait ControllersModule extends ServicesModule {

  lazy val userController: UserController = wire[UserController]

  lazy val homeController: HomeController = wire[HomeController]

  def langs: Langs

  def controllerComponents: ControllerComponents
}