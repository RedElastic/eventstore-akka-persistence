package domain.chat

import java.time.LocalDateTime
import java.util.UUID

import domain.user.User.UserId

case class ChatMatch(user1: UserId, user2: UserId) extends domain.Event
object ChatMatch {
  def apply(_ts: LocalDateTime, _eventId: UUID, user1: UserId, user2: UserId) = {
    new ChatMatch(user1,user2) {
      override val ts = _ts
      override val eventId: UUID = _eventId
    }
  }
}
