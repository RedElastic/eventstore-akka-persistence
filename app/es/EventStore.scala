package es

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.event.Logging
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Attributes}
import akka.{NotUsed, actor => untyped}
import domain.user.User.Command.ChatConnect
import domain.user.User.UserId
import eventstore.tcp.ConnectionActor
import eventstore.{EventStream, _}
import play.api.libs.json.Json

object EventStore {
  case class Config(topics: TopicsConfig)
  case class TopicsConfig(partnerFoundPrefix: String)
}


class EventStore(connection: EsConnection, config: EventStore.Config)(implicit system: untyped.ActorSystem) {

  private val log = play.api.Logger
  private implicit val loggingAdapter = system.log



//  import system.dispatcher
//  implicit val materializer = akka.stream.ActorMaterializer()
  def users: Source[Event, NotUsed] = connection.streamSource(
    streamId = EventStream.Id("$ce-user"),
    infinite = true,
    resolveLinkTos = true)

  def connect(user1: UserId, user2: UserId): Unit = {

    val event = EventData(
      eventType = "partner-match",
      data = Content("my event data"))

    write(EventStream.Id("chats"), event)
  }

  def write(stream: EventStream.Id, event: EventData): Unit = {
    val connection = system.actorOf(ConnectionActor.props())
    implicit val writeResult = system.actorOf(Props[WriteResult])

    connection ! WriteEvents(stream, List(event))

    class WriteResult extends Actor with ActorLogging {
      def receive = {
        case WriteEventsCompleted(range, position) =>
          log.info("range: {}, position: {}", range, position)
          context.system.terminate()

        case Failure(e: EsException) =>
          log.error(e.toString)
          context.system.terminate()
      }
    }

  }

  def chatsStream(userId: UserId): Source[ChatConnect, NotUsed] = {
    implicit val mat = ActorMaterializer()
    val streamId = s"${config.topics.partnerFoundPrefix}-${userId.persistenceId}"
    log.info(s"Subscribing to $streamId")
    connection.streamSource(
      streamId = EventStream.Id(streamId),
      fromEventNumberExclusive = Some(EventNumber.Last),    // More generally we may need the aggregate to maintain it's offset
      infinite = true,
      resolveLinkTos = true)
      .log(streamId, _.toString).withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .collect {
        case e: eventstore.Event if e.data.eventType == "PartnerFound" =>
          e.data.data match {
            case eventstore.Content.Json(js) =>
              import serde.ChatConnectSerDe._
              Json.parse(js).as[ChatConnect]
          }

      }
  }
}
