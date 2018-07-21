package es.serde

import domain.chat.ChatMatch
import domain.user.UserEvent
import domain.user.UserEvent.Banned
import domain.user.User.UserId
import es.serde.Json4sSerializer._
import org.json4s._
import org.json4s.native.Serialization
import org.scalatest.{MustMatchers, WordSpec}

class Json4sSerializerTest extends WordSpec with MustMatchers {

  implicit val formats = Serialization.formats(NoTypeHints) + BannedSerializer + MatchedWIthPartnerSerializer

  "UserEventSerializer" should {
    " serialization > deserialization" in {
      val event = Banned()
      Extraction.decompose(event).extract[Event] mustBe event
    }
    "" in {

      val cm = ChatMatch(UserId("u1"), UserId("u2"))
      Extraction.decompose(cm).extract[ChatMatch] mustBe cm

    }
  }
}
