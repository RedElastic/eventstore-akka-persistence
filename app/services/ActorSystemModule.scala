package services

import akka.actor.{ActorSystem, typed}

trait ActorSystemModule {
  import akka.actor.typed.scaladsl.adapter._

  implicit def actorSystem: ActorSystem
  implicit lazy val typedActorSystem: typed.ActorSystem[Nothing] = actorSystem.toTyped
}
