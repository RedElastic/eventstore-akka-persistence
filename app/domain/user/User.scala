package domain.user

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.typed.scaladsl.PersistentBehaviors._
import akka.persistence.typed.scaladsl._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.Timeout
import domain.user.User.UserId
import es.EventStore

import scala.concurrent.Future
import scala.concurrent.duration._


trait UserRepository {
  def ban(userId: UserId): Future[Unit]
  def ping(userId: UserId): Future[Unit]
}

class UserRepositoryImpl(readStream: EventStore)(implicit actorSystem: ActorSystem[Nothing]) extends UserRepository {

  implicit val timeout = Timeout(2.seconds)
  implicit val ec = actorSystem.executionContext

  def ping(userId: UserId) = {
    routingActor.map(_ ! GetRef(userId))
  }

  def ban(userId: UserId) = {
    routingActor.map(_ ! UserCommand(userId, User.Command.Ban))
  }

  sealed trait RouterMessage
  case class UserCommand(userId: UserId, cmd: User.Command) extends RouterMessage
  case class UserRemoved(userId: UserId) extends RouterMessage
  case class GetRef(userId: UserId) extends RouterMessage

  private def getRef(userId: UserId,
                     users: Map[UserId, ActorRef[User.Command]])(
      implicit ctx: ActorContext[RouterMessage]): (ActorRef[User.Command], Map[UserId, ActorRef[User.Command]]) = {
    users.get(userId) match {
      case Some(ref) =>
        (ref, users)
      case None =>
        val ref = ctx.spawnAnonymous(User.behavior(userId, readStream))
        ctx.watchWith(ref, UserRemoved(userId))
        (ref, users + (userId -> ref))
    }
  }

  private def routing(users: Map[UserId, ActorRef[User.Command]]): Behavior[RouterMessage] = Behaviors.receive {
    case (ctx, UserCommand(userId, msg)) =>
      val (userRef, userpool) = getRef(userId, users)(ctx)
      userRef ! msg
      routing(userpool)
    case (ctx, UserRemoved(userId)) =>
      routing(users - userId)
    case (ctx, GetRef(userId)) =>
      val (userRef, userpool) = getRef(userId, users)(ctx)
      routing(userpool)
  }

  private val routingActor = actorSystem.systemActorOf(routing(Map.empty), name = "user-router")

}


// It be nice to put this in User but there are serialization issues that I don't care to work out at the moment.
abstract class Event(val eventType: String) extends domain.Event
object Event {
  case class Banned() extends Event("Banned")
  object Banned {
    def apply(_ts: LocalDateTime, _eventId: UUID): Banned = new Banned() {
      override val ts: LocalDateTime = _ts
      override val eventId: UUID = _eventId
    }
  }
  case class MatchedWithPartner(partnerId: UserId) extends Event("MatchedWithPartner")
}

object User {

  case class UserId(asString: String) extends AnyVal {
    def persistenceId = s"user-$asString"
  }

  sealed trait Command
  object Command {
    case object Ban extends Command
    case class ChatConnect(partnerId: UserId) extends Command
  }


  sealed trait BanStatus
  object BanStatus {
    case object Banned extends BanStatus
    case object NotBanned extends BanStatus
  }

  sealed trait UserState
  case object BannedState extends UserState
  case object Idle extends UserState
  case class Chatting(partnerId: UserId) extends UserState

  import Command._
  import Event._

  def behavior(userId: UserId, readStream: EventStore): Behavior[User.Command] = Behaviors.setup { ctx =>
    import akka.actor.typed.scaladsl.adapter._
    implicit val system = ctx.system.toUntyped
    implicit val mat = ActorMaterializer()
    readStream.chatsStream(userId).runWith(
      Sink.foreach { msg =>
        play.api.Logger.info(msg.toString)
        ctx.self ! msg
      })
    persistentBehavior(userId)
  }

  def persistentBehavior(userId: UserId): Behavior[Command] =
    PersistentBehaviors.receive[Command, Event, UserState](
      persistenceId = userId.persistenceId,
      emptyState = Idle,
      commandHandler = commandHandler(userId),
      eventHandler = eventHandler)
    .snapshotEvery(numberOfEvents = 20)
    .onRecoveryCompleted{ (ctx, state) =>
      ctx.log.info(s"Recovered: ${state.toString}")
    }

  def commandHandler(userId: UserId): CommandHandler[Command, Event, UserState] = (ctx,state,command) =>
    state match {
      case Idle =>
        command match {
          case Ban =>
            ctx.log.info(s"User ${userId.asString} banned.")
            Effect.persist(Banned())
          case ChatConnect(partnerId) =>
            ctx.log.info(s"User ${userId.asString} matched with ${partnerId.asString}")
            Effect.persist(MatchedWithPartner(partnerId))
        }
      case Chatting(partnerId) =>
        command match {
          case Ban =>
            Effect.persist(Banned())
          case ChatConnect(_) =>
            Effect.unhandled
        }
      case BannedState =>
        Effect.unhandled
    }

  val eventHandler: (UserState, Event) => UserState = {
    case (state, Banned()) =>
      BannedState
    case (Idle, MatchedWithPartner(partnerId)) =>
      Chatting(partnerId)
    case (state,_) =>
      state
  }

}