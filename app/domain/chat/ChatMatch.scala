package domain.chat

import java.time.Instant
import java.util.UUID

import domain.user.User.UserId

case class ChatMatch(user1: UserId, user2: UserId) extends domain.Event("ChatMatch")
object ChatMatch {
  def apply(_ts: Instant, _eventId: UUID, user1: UserId, user2: UserId): ChatMatch = {
    new ChatMatch(user1,user2) {
      override val ts = _ts
      override val eventId: UUID = _eventId
    }
  }
}
