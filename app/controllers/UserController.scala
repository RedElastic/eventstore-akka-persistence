package controllers

import domain.user.User.UserId
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import services.user.UserService

class UserController(cc: ControllerComponents,
                               userService: UserService) extends AbstractController(cc) {
  def ban(userId: String) = Action { request: Request[AnyContent] =>
    userService.ban(UserId(userId))
//    import org.json4s.native.Serialization.{read, write}
//    implicit val formats = DefaultFormats + UserEventSerializer
//    import domain.user.Event.Banned
//    val b = write(Banned)
    Ok("done")
  }
}
