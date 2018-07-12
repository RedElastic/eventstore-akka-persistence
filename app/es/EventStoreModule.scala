package es

import com.softwaremill.macwire._
import eventstore.EventStoreExtension
import services.ActorSystemModule

trait EventStoreModule extends ActorSystemModule {
  lazy val esConnection = wire[EventStoreExtension].connection

  lazy val eventStore: EventStore = wire[EventStore]
}
