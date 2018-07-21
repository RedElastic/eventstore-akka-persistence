package es.serde

import java.nio.ByteBuffer
import java.nio.charset.Charset

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.persistence.eventstore.EventStoreSerializer
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent.Snapshot
import akka.persistence.{PersistentRepr, SnapshotMetadata}
import akka.util.ByteString
import eventstore.{Content, ContentType, Event, EventData}
import org.json4s.Extraction.decompose
import org.json4s._
import org.json4s.native.Serialization.{read, write}

class Json4sSerializer(val system: ExtendedActorSystem) extends EventStoreSerializer {
  import Json4sSerializer._

  implicit val formats =
    DefaultFormats +
      SnapshotSerializer +
      new PersistentReprSerializer(system) +
      ActorRefSerializer ++
      UserEvents.serializers

  def identifier = Identifier

  def includeManifest = true

  def fromBinary(bytes: Array[Byte], manifestOpt: Option[Class[_]]) = {
    implicit val manifest = manifestOpt match {
      case Some(x) => Manifest.classType(x)
      case None    => Manifest.AnyRef
    }
    read(new String(bytes, UTF8))
  }

  def toBinary(o: AnyRef) = write(o).getBytes(UTF8)

  def toEvent(x: AnyRef) = x match {
    case x: PersistentRepr =>
      val data = x.payload.asInstanceOf[AnyRef]
      val metadata = x.withPayload("").withManifest(classFor(x.payload.asInstanceOf[AnyRef]).getName)
      EventData(
        eventType = classFor(x).getName,
        data = Content(ByteString(toBinary(data)), ContentType.Json),
        metadata = Content(ByteString(toBinary(metadata)), ContentType.Json)
      )

    case x: SnapshotEvent => EventData(
      eventType = classFor(x).getName,
      data = Content(ByteString(toBinary(x)), ContentType.Json)
    )

    case _ => sys.error(s"Cannot serialize $x, SnapshotEvent expected")
  }

  def fromEvent(event: Event, manifest: Class[_]) = {
    //val clazz = Class.forName(event.data.eventType)
    val metadata = fromBinary(event.data.metadata.value.toArray, manifest)
    metadata match {
      case x: PersistentRepr =>
        val payload = fromBinary(event.data.data.value.toArray, Class.forName(x.manifest))
        x.withPayload(payload)
      case result =>
        if (manifest.isInstance(result)) result
        else sys.error(s"Cannot deserialize event as $manifest, event: $event")
    }

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

object Json4sSerializer {
  val UTF8: Charset = Charset.forName("UTF-8")
  val Identifier: Int = ByteBuffer.wrap("json4s".getBytes(UTF8)).getInt

  object SnapshotSerializer extends Serializer[Snapshot] {
    val Clazz = classOf[Snapshot]

    def deserialize(implicit format: Formats) = {
      case (TypeInfo(Clazz, _), JObject(List(
      JField("data", JString(x)),
      JField("metadata", metadata)))) => Snapshot(x, metadata.extract[SnapshotMetadata])
    }

    def serialize(implicit format: Formats) = {
      case Snapshot(data, metadata) => JObject("data" -> JString(data.toString), "metadata" -> decompose(metadata))
    }
  }

  class PersistentReprSerializer(system: ExtendedActorSystem) extends Serializer[PersistentRepr] {
    val Clazz = classOf[PersistentRepr]

    def deserialize(implicit format: Formats) = {
      case (TypeInfo(Clazz, _), json) =>
        val mapping = json.extract[Mapping]
        // Clunky, needs fixing. The EventStore docs didn't really seem to cover this.
        /*
        val payload = mapping.manifest match {
          case "domain.user.Event$MatchedWithPartner" =>
            (json \ "payload").extract[domain.user.Event.MatchedWithPartner]
          case "domain.user.Event$Banned" =>
            (json \ "payload").extract[domain.user.Event.Banned]
          case _ =>
            mapping.payload
        }
        */
        PersistentRepr(
          payload = null,
          sequenceNr = mapping.sequenceNr,
          persistenceId = mapping.persistenceId,
          manifest = mapping.manifest,
          writerUuid = mapping.writerUuid
        )
    }
    def serialize(implicit format: Formats) = {
      case x: PersistentRepr =>
        //val manifest = x.payload.getClass.getName
        val mapping = Mapping(
          payload = x.payload.asInstanceOf[AnyRef],
          sequenceNr = x.sequenceNr,
          persistenceId = x.persistenceId,
          manifest = x.manifest,
          writerUuid = x.writerUuid
        )
        decompose(mapping)
    }
  }



  case class Mapping(
                      payload:       AnyRef,
                      sequenceNr:    Long,
                      persistenceId: String,
                      manifest:      String,
                      writerUuid:    String
                    )
}
