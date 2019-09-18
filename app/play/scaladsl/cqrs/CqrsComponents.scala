/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.typed.scaladsl.EventSourcedBehavior

import scala.reflect.ClassTag
import akka.annotation.ApiMayChange
import akka.persistence.typed.PersistenceId
import model.AccountCommand

@ApiMayChange
trait CqrsComponents {

  def actorSystem: ActorSystem

  final def createEntityFactory[Command: ClassTag, Event, State](
      name: String,
      behaviorFunc: (
          ActorContext[Command],
          PersistenceId) => EventSourcedBehavior[Command, Event, State],
      tagger: Tagger[Event]
  ): EntityFactory[Command, Event, State] =
    new EntityFactory(name, behaviorFunc, tagger, actorSystem)

}
