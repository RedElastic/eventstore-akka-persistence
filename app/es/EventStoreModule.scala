package es

import com.softwaremill.macwire._
import eventstore.{EsConnection, EventStoreExtension}
import pureconfig.error.ConfigReaderException
import services.ActorSystemModule

trait EventStoreModule extends ActorSystemModule {
  val config: EventStore.Config = pureconfig.loadConfig[EventStore.Config](namespace = "eventstore").fold(
    err => throw new ConfigReaderException[EventStore.Config](err),
    identity
  )

  lazy val esConnection: EsConnection = wire[EventStoreExtension].connection

  lazy val eventStore: EventStore = wire[EventStore]
}