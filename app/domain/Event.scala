package domain

import java.time.LocalDateTime
import java.util.UUID

trait Event {
  val ts: LocalDateTime = LocalDateTime.now()
  val eventId: UUID = UUID.randomUUID()
}

