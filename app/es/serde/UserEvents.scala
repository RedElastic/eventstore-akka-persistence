package es.serde

import org.json4s._

object UserEvents {
  val serializers = Seq(MatchedWithPartnerSerializer, BannedSerializer)

  import domain.user
  import java.time.Instant
  import java.util.UUID
  import domain.user.User.UserId
  object MatchedWithPartnerSerializer extends CustomSerializer[domain.user.UserEvent.MatchedWithPartner](implicit format => (
    {
      case jObj: JObject =>
        val _ts = Instant.parse((jObj \ "ts").extract[String])
        val _eventId = UUID.fromString((jObj \ "eventId").extract[String])
        val partnerId = UserId((jObj \ "partnerId").extract[String])
        new user.UserEvent.MatchedWithPartner(partnerId) {
          override val eventId: UUID = _eventId
          override val ts: Instant = _ts
        }
    }, {
    case matchedWithPartner: user.UserEvent.MatchedWithPartner =>
      JObject(
        "partnerId" -> JString(matchedWithPartner.partnerId.asString),
        "ts" -> JString(matchedWithPartner.ts.toString),
        "eventId" -> JString(matchedWithPartner.eventId.toString)
      )

  }))

  object BannedSerializer extends CustomSerializer[domain.user.UserEvent.Banned](implicit format => (
    {
      case jObj: JObject =>
        val _ts = Instant.parse((jObj \ "ts").extract[String])
        val _eventId = UUID.fromString((jObj \ "eventId").extract[String])
        new user.UserEvent.Banned() {
          override val eventId: UUID = _eventId
          override val ts: Instant = _ts
        }
    }, {
    case banned: user.UserEvent.Banned =>
      JObject(
        "ts" -> JString(banned.ts.toString),
        "eventId" -> JString(banned.eventId.toString)
      )
  }))

}
