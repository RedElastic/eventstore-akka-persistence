package services.user

import com.softwaremill.macwire.wire

trait UserServiceModule extends UserRepositoryModule {

  lazy val userService = wire[UserService]

}
