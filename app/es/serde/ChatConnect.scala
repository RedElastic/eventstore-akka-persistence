package es.serde

import domain.user.User.Command.ChatConnect
import domain.user.User.UserId
import play.api.libs.json._

object ChatConnectSerDe {
  implicit val readUserId: Reads[UserId] = {
    json: JsValue => json.validate[String].map(UserId.apply)
  }
  implicit val readChatConnect: Reads[ChatConnect] = {
    (__ \ "partnerId").read[UserId].map(ChatConnect.apply)
  }
}
