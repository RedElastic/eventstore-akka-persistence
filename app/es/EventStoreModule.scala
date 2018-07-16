package es

import com.softwaremill.macwire._
import eventstore.{EsConnection, EventStoreExtension}
import services.ActorSystemModule

trait EventStoreModule extends ActorSystemModule {
  val config: EventStore.Config = pureconfig.loadConfigOrThrow[EventStore.Config](namespace = "eventstore")

  lazy val esConnection: EsConnection = wire[EventStoreExtension].connection

  lazy val eventStore: EventStore = wire[EventStore]
}