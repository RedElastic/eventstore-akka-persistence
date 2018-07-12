package es

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import eventstore.EventStoreExtension
import javax.inject.Singleton

class Module extends AbstractModule {

  @Provides
  @Singleton
  def esConnection(actorSystem: ActorSystem) = EventStoreExtension(actorSystem).connection

  override def configure(): Unit = {}
}
