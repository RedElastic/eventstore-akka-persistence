package es.serde

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.UUID

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.persistence.eventstore.EventStoreSerializer
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent.Snapshot
import akka.persistence.{PersistentRepr, SnapshotMetadata}
import akka.util.ByteString
import domain.chat.ChatMatch
import domain.user.User.UserId
import eventstore.{Content, ContentType, Event, EventData}
import org.json4s.Extraction.decompose
import org.json4s._
import org.json4s.native.Serialization.{read, write}

class Json4sSerializerX(val system: ExtendedActorSystem) extends EventStoreSerializer {
  import Json4sSerializerX._

  implicit val formats = DefaultFormats +
    SnapshotSerializer +
    new PersistentReprSerializer(system) +
    ActorRefSerializer +
    EventSerializer

  override val identifier = Identifier

  override val includeManifest = true

  def fromBinary(bytes: Array[Byte], manifestOpt: Option[Class[_]]): AnyRef = {
    implicit val manifest = manifestOpt match {
      case Some(x) => Manifest.classType(x)
      case None    => Manifest.AnyRef
    }
    read(new String(bytes, UTF8))
  }

  def toBinary(o: AnyRef) = write(o).getBytes(UTF8)

  def toEvent(x: AnyRef) = {
    x match {
      case x: PersistentRepr => EventData(
        eventType = classFor(x).getName,
        data = Content(ByteString(toBinary(x)), ContentType.Json)
      )

      case x: SnapshotEvent => EventData(
        eventType = classFor(x).getName,
        data = Content(ByteString(toBinary(x)), ContentType.Json)
      )

      case _ => sys.error(s"Cannot serialize $x, SnapshotEvent expected")
    }
  }

  def fromEvent(event: Event, manifest: Class[_]) = {
    val clazz = Class.forName(event.data.eventType)
    val result = fromBinary(event.data.data.value.toArray, clazz)
    if (manifest.isInstance(result)) result
    else sys.error(s"Cannot deserialize event as $manifest, event: $event")
  }

  def classFor(x: AnyRef) = x match {
    case x: PersistentRepr => classOf[PersistentRepr]
    case _                 => x.getClass
  }

  object ActorRefSerializer extends Serializer[ActorRef] {
    val Clazz = classOf[ActorRef]

    def deserialize(implicit format: Formats) = {
      case (TypeInfo(Clazz, _), JString(x)) => system.provider.resolveActorRef(x)
    }

    def serialize(implicit format: Formats) = {
      case x: ActorRef => JString(x.path.toSerializationFormat)
    }
  }
}

object Json4sSerializerX {
  val UTF8: Charset = Charset.forName("UTF-8")
  val Identifier: Int = ByteBuffer.wrap("json4s".getBytes(UTF8)).getInt

  object SnapshotSerializer extends Serializer[Snapshot] {
    val Clazz = classOf[Snapshot]

    def deserialize(implicit format: Formats) = {
      case (TypeInfo(Clazz, _), JObject(List(
      JField("data", JString(x)),
      JField("metadata", metadata)))) =>
        Snapshot(x, metadata.extract[SnapshotMetadata])
    }

    def serialize(implicit format: Formats) = {
      case Snapshot(data, metadata) =>
        JObject("data" -> JString(data.toString), "metadata" -> decompose(metadata))
    }
  }

  class PersistentReprSerializer(system: ExtendedActorSystem) extends Serializer[PersistentRepr] {
    val Clazz = classOf[PersistentRepr]

    def deserialize(implicit format: Formats) = {
      case (TypeInfo(Clazz, _), json) =>
        val x = json.extract[Mapping]
        val payload = x.manifest match {
          case "domain.user.Event$MatchedWithPartner" =>
            read[domain.user.Event.MatchedWithPartner](x.payload.asInstanceOf[String])
          case _ =>
            x.payload
        }
        PersistentRepr(
          payload = payload,
          sequenceNr = x.sequenceNr,
          persistenceId = x.persistenceId,
          manifest = x.manifest,
          writerUuid = x.writerUuid
        )
    }
    def serialize(implicit format: Formats) = {
      case x: PersistentRepr =>
        val mapping = Mapping(
          payload = x.payload,
          sequenceNr = x.sequenceNr,
          persistenceId = x.persistenceId,
          manifest = x.manifest,
          writerUuid = x.writerUuid
        )
        decompose(mapping)
    }

  }

  import domain.user
  import java.time.format.DateTimeFormatter
  val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  object EventSerializer extends CustomSerializer[user.Event](implicit format => (
    {
      case jObj: JObject if (jObj \ "type").extract[String] == "Banned" =>
        val payload = jObj
        (jObj \ "payload" \ "type").extract[String] match {
          case "Banned" =>
            val ts = LocalDateTime.parse((jObj \ "ts").extract[String], dateTimeFormatter)
            val eventId = UUID.fromString((jObj \ "eventId").extract[String])
            user.Event.Banned(ts, eventId)
        }
      case jObj: JObject if (jObj \ "type").extract[String] == "MatchedWithPartner" =>
        val payload = jObj
        val _ts = LocalDateTime.parse((payload \ "ts").extract[String], dateTimeFormatter)
        val _eventId = UUID.fromString((payload \ "eventId").extract[String])
        val partnerId = UserId((payload \ "partnerId").extract[String])
        new user.Event.MatchedWithPartner(partnerId) {
          override val eventId: UUID = _eventId
          override val ts: LocalDateTime = _ts
        }
      case x =>
        println(x)
        ???
    },
    {
      case banned: user.Event.Banned =>
        JObject(
          "type" -> JString("Banned"),
          "ts" -> JString(dateTimeFormatter.format(banned.ts)),
          "eventId" -> JString(banned.eventId.toString)
        )
      case matchedWithPartner: user.Event.MatchedWithPartner =>
        JObject(
          "type" -> JString("MatchedWithPartner"),
          "partnerId" -> JString(matchedWithPartner.partnerId.asString),
          "ts" -> JString(dateTimeFormatter.format(matchedWithPartner.ts)),
          "eventId" -> JString(matchedWithPartner.eventId.toString)
        )
    }
  ))

  import domain.chat
  object ChatConnectSerializer extends CustomSerializer[ChatMatch](implicit format => (
    {
      case jObj: JObject if (jObj \ "type").extract[String] == "ChatMatch" =>
        val ts = (jObj \ "ts").extract[LocalDateTime]
        val eventId = (jObj \ "eventId").extract[UUID]
        val user1 = (jObj \ "user1").extract[String]
        val user2 = (jObj \ "user2").extract[String]
        chat.ChatMatch(ts, eventId, UserId(user1), UserId(user2))
    },
    {
      case e@chat.ChatMatch(u1, u2) =>
        JObject(
          "type" -> JString("ChatMatch"),
          "ts" -> JString(e.ts.toString),
          "eventId" -> JString(e.eventId.toString),
          "user1" -> JString(u1.asString),
          "user2" -> JString(u2.asString)
        )
    }
  ))

  case class Mapping(
                      payload:       Any,
                      sequenceNr:    Long,
                      persistenceId: String,
                      manifest:      String,
                      writerUuid:    String
                    )
}

