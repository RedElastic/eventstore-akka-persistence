package controllers

import domain.user.User.UserId
import domain.user.UserRepository
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import play.api.Logger

class UserController(cc: ControllerComponents,
                     userRepository: UserRepository) extends AbstractController(cc) {

  private val log = Logger
  def ban(userId: String) = Action { request: Request[AnyContent] =>
    log.info(s"User $userId ban action")
    userRepository.ban(UserId(userId))
    //    import org.json4s.native.Serialization.{read, write}
    //    implicit val formats = DefaultFormats + UserEventSerializer
    //    import domain.user.Event.Banned
    //    val b = write(Banned)
    Ok("done")
  }

  def ping(userId: String) = Action { request: Request[AnyContent] =>
    log.info(s"User $userId ping action")
    userRepository.ping(UserId(userId))
    //    import org.json4s.native.Serialization.{read, write}
    //    implicit val formats = DefaultFormats + UserEventSerializer
    //    import domain.user.Event.Banned
    //    val b = write(Banned)
    Ok("done")
  }
}
