package es.serde

import akka.actor.ExtendedActorSystem
import akka.persistence.eventstore.EventStoreSerializer
import eventstore.{Event, EventData}
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}

class PlayJsonSerializer(val system: ExtendedActorSystem) extends EventStoreSerializer {
  override def toEvent(o: AnyRef): EventData = ???

  override def fromEvent(event: Event, manifest: Class[_]): AnyRef = ???

  override def identifier: Int = ???

  override def toBinary(o: AnyRef): Array[Byte] = ???

  override def includeManifest: Boolean = ???

  override def fromBinary(bytes: Array[Byte], manifestOpt: Option[Class[_]]): AnyRef = {


    Json.parse(bytes)
    ???
  }

  def parse[T <: AnyRef](json: JsValue, manifestOpt: Option[Class[_]]): T = {
    ???
  }
}
