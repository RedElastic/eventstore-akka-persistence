import akka.actor.typed.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import akka.{actor => untyped}

class Module extends AbstractModule {

  @Provides
  def actorSystem(actorSystem: untyped.ActorSystem): ActorSystem[Nothing] = {
    ActorSystem.wrap(actorSystem)
  }


  override def configure(): Unit = {}
}
