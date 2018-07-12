package es

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.{NotUsed, actor => untyped}
import domain.user.User.Command.ChatConnect
import domain.user.User.UserId
import eventstore.tcp.ConnectionActor
import eventstore.{EventStream, _}

class EventStore(connection: EsConnection)(implicit system: untyped.ActorSystem) {

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
    connection.streamSource(
      streamId = EventStream.Id(s"chats-${userId.persistenceId}"),
      infinite = true,
      resolveLinkTos = true)
      .collect {
        case e: eventstore.Event if e.data.eventType == "ChatConnect" =>
          e.data.data match {
            case eventstore.Content.Json(js) =>
              implicit val format = org.json4s.DefaultFormats
              import org.json4s.native.Serialization.read
              val msg: ChatConnect = read[ChatConnect](js)
              msg
          }

      }
  }
}
