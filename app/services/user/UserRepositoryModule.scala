package services.user

import com.softwaremill.macwire.wire
import domain.user.{UserRepository, UserRepositoryImpl}
import es.EventStore
import eventstore.{EsConnection, EventStoreExtension}
import services.ActorSystemModule

trait UserRepositoryModule extends ActorSystemModule {

  def esConnection: EsConnection = wire[EventStoreExtension].connection

  lazy val eventStore: EventStore = wire[EventStore]
  lazy val userRepository: UserRepository = wire[UserRepositoryImpl]
}
