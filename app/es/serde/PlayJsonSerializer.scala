/*
package es.serde

import java.nio.ByteBuffer
import java.nio.charset.Charset

import akka.actor.ExtendedActorSystem
import akka.persistence.PersistentRepr
import akka.persistence.eventstore.EventStoreSerializer
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.util.ByteString
import eventstore.{Content, ContentType, Event, EventData}
import play.api.libs.json.{Format, Json}

class PlayJsonSerializer(system: ExtendedActorSystem) extends PlayJsonSerializerImpl(system) {
  override val formatterRegistry: JsonFormatterRegistry = JsonFormatterRegistry(Map(
    classOf[Event] -> Json.format[Event]
  ))
}

case class JsonFormatterRegistry(formatter: Map[Class[_], Format[_]]) {
  def get(clazz: Class[_]): Format[AnyRef] = {
    formatter(clazz).asInstanceOf[Format[AnyRef]]
  }
}

abstract class PlayJsonSerializerImpl(val system: ExtendedActorSystem) extends EventStoreSerializer {
  import PlayJsonSerializer._

  val formatterRegistry: JsonFormatterRegistry

  def toEvent(x: AnyRef) = x match {
    case x: PersistentRepr => EventData(
      eventType = classFor(x.payload.asInstanceOf[AnyRef]).getName,
      data = Content(ByteString(toBinary(x)), ContentType.Json)
    )

    case x: SnapshotEvent => EventData(
      eventType = classFor(x).getName,
      data = Content(ByteString(toBinary(x)), ContentType.Json)
    )

    case _ => sys.error(s"Cannot serialize $x, SnapshotEvent expected")
  }

  def fromEvent(event: Event, manifest: Class[_]) = {
    val clazz = Class.forName(event.data.eventType)
    val result = fromBinary(event.data.data.value.toArray, clazz)
    if (manifest.isInstance(result)) result
    else sys.error(s"Cannot deserialize event as $manifest, event: $event")
  }

  override def identifier: Int = Identifier

  override def toBinary(o: AnyRef): Array[Byte] = Json.toBytes(Json.toJson(o))

  override val includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifestOpt: Option[Class[_]]): AnyRef = {
    implicit val manifest = manifestOpt match {
      case Some(x) => Manifest.classType(x)
      case None    => Manifest.AnyRef
    }
    val json = Json.parse(bytes)
    formatterRegistry.formatter(manifest.getClass).reads(json).get // FIXME: BOOM!
  }

  def classFor(x: AnyRef) = x match {
    case x: PersistentRepr => classOf[PersistentRepr]
    case _                 => x.getClass
  }
}

object PlayJsonSerializer {
  val UTF8: Charset = Charset.forName("UTF-8")
  val Identifier: Int = ByteBuffer.wrap("play-json".getBytes(UTF8)).getInt
}
*/