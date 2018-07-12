package controllers

import domain.user.User.UserId
import domain.user.UserRepository
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

@Singleton
class UserController @Inject()(cc: ControllerComponents,
                               userRepository: UserRepository) extends AbstractController(cc) {
  def ban(userId: String) = Action { request: Request[AnyContent] =>
    userRepository.ban(UserId(userId))
//    import org.json4s.native.Serialization.{read, write}
//    implicit val formats = DefaultFormats + UserEventSerializer
//    import domain.user.Event.Banned
//    val b = write(Banned)
    Ok("done")
  }
}
