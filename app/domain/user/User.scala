package domain.user

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.persistence.typed.scaladsl.PersistentBehaviors._
import akka.persistence.typed.scaladsl._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.Timeout
import com.google.inject.ImplementedBy
import domain.user.User.UserId
import es.EventStore
import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._

@ImplementedBy(classOf[UserRepositoryImpl])
trait UserRepository {
  def ban(userId: UserId): Future[Unit]
}

class UserRepositoryImpl @Inject()(readStream: EventStore)(implicit actorSystem: ActorSystem[Nothing]) extends UserRepository {

  implicit val timeout = Timeout(2.seconds)
  implicit val ec = actorSystem.executionContext

  def ban(userId: UserId) = {
    routingActor.map(_ ! (userId, User.Command.Ban))
  }

  private def routing(users: Map[UserId, ActorRef[User.Command]]): Behavior[(UserId, User.Command)] = Behaviors.receive {
    case (ctx, (userId, msg)) =>
      val ref = users.getOrElse(userId, ctx.spawnAnonymous(User.behavior(userId, readStream)))
      ref ! msg
      Behavior.same
  }

  private val routingActor = actorSystem.systemActorOf(routing(Map.empty), name = "user-router")

}


// It be nice to put this in User but there are serialization issues that I don't care to work out at the moment.
sealed trait Event extends domain.Event
object Event {
  case class Banned() extends Event
  object Banned {
    def apply(_ts: LocalDateTime, _eventId: UUID): Banned = new Banned() {
      override val ts: LocalDateTime = _ts
      override val eventId: UUID = _eventId
    }
  }
  case class MatchedWithPartner(partnerId: UserId) extends Event
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
      Sink.foreach(msg => ctx.log.info(msg.toString)))
    persistentBehavior(userId)
  }

  def persistentBehavior(userId: UserId): Behavior[Command] =
    PersistentBehaviors.receive[Command, Event, UserState](
      persistenceId = userId.persistenceId,
      emptyState = Idle,
      commandHandler = commandHandler,
      eventHandler = eventHandler)

  val commandHandler: CommandHandler[Command, Event, UserState] = CommandHandler.byState {
    case Idle =>
      CommandHandler.command {
        case Ban =>
          Effect.persist(Banned())
        case ChatConnect(partnerId) =>
          Effect.persist(MatchedWithPartner(partnerId))
      }
    case Chatting(partnerId) =>
      CommandHandler.command {
        case Ban =>
          Effect.persist(Banned())
        case ChatConnect(_) =>
          Effect.none
      }
    case BannedState =>
      CommandHandler.command {
        _ =>
          Effect.none
      }


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