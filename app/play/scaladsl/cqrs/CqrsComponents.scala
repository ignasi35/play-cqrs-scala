/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs
import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.scaladsl.EventSourcedBehavior

import scala.reflect.ClassTag
import akka.annotation.ApiMayChange

@ApiMayChange
trait CqrsComponents {

  def actorSystem: ActorSystem

  final def createEntityFactory[Command: ClassTag, Event, State](
      name: String,
      behaviorFunc: EntityContext => EventSourcedBehavior[Command,
                                                          Event,
                                                          State],
      tagger: Tagger[Event]
  ): EntityFactory[Command, Event, State] =
    new EntityFactory(name, behaviorFunc, tagger, actorSystem)

}
