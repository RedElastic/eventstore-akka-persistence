package domain

import java.time.Instant
import java.util.UUID

abstract class Event(val eventName: String) {
  val ts: Instant = Instant.now()
  val eventId: UUID = UUID.randomUUID()
}

