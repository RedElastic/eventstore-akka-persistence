package es.serde

import org.json4s._

object UserEvents {
  val serializers = Seq(MatchedWithPartnerSerializer, BannedSerializer)

  import domain.user
  import java.time.LocalDateTime
  import java.util.UUID
  import domain.user.User.UserId
  import java.time.format.DateTimeFormatter
  val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  object MatchedWithPartnerSerializer extends CustomSerializer[domain.user.Event.MatchedWithPartner](implicit format => (
    {
      case jObj: JObject if (jObj  \ "type").extract[String] == "MatchedWithPartner" =>
        val _ts = LocalDateTime.parse((jObj \ "ts").extract[String], dateTimeFormatter)
        val _eventId = UUID.fromString((jObj \ "eventId").extract[String])
        val partnerId = UserId((jObj \ "partnerId").extract[String])
        new user.Event.MatchedWithPartner(partnerId) {
          override val eventId: UUID = _eventId
          override val ts: LocalDateTime = _ts
        }
    }, {
    case matchedWithPartner: user.Event.MatchedWithPartner =>
      JObject(
        "type" -> JString("MatchedWithPartner"),
        "partnerId" -> JString(matchedWithPartner.partnerId.asString),
        "ts" -> JString(dateTimeFormatter.format(matchedWithPartner.ts)),
        "eventId" -> JString(matchedWithPartner.eventId.toString)
      )

  }))

  object BannedSerializer extends CustomSerializer[domain.user.Event.Banned](implicit format => (
    {
      case jObj: JObject if (jObj  \ "type").extract[String] == "Banned" =>
        val _ts = LocalDateTime.parse((jObj \ "ts").extract[String], dateTimeFormatter)
        val _eventId = UUID.fromString((jObj \ "eventId").extract[String])
        new user.Event.Banned() {
          override val eventId: UUID = _eventId
          override val ts: LocalDateTime = _ts
        }
    }, {
    case banned: user.Event.Banned =>
      JObject(
        "type" -> JString("Banned"),
        "ts" -> JString(dateTimeFormatter.format(banned.ts)),
        "eventId" -> JString(banned.eventId.toString)
      )
  }))

}
