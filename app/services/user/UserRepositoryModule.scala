package services.user

import com.softwaremill.macwire.wire
import domain.user.{UserRepository, UserRepositoryImpl}
import es.EventStoreModule
import services.ActorSystemModule

trait UserRepositoryModule extends ActorSystemModule with EventStoreModule {

  lazy val userRepository: UserRepository = wire[UserRepositoryImpl]
}
