/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.journal.Tagged
import akka.persistence.typed.ExpectingReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import scala.reflect.ClassTag
import akka.annotation.ApiMayChange

@ApiMayChange
class CqrsEntityFactory[Command: ClassTag, Event, State](
    name: String,
    typeKey: EntityTypeKey[Command],
    behaviorFunc: EntityContext => EventSourcedBehavior[Command, Event, State],
    tagger: Tagger[Event],
    clusterSharding: ClusterSharding
) {

  def entityRefFor(entityId: String): EntityRef[Command] =
    clusterSharding.entityRefFor(typeKey, entityId)

  // TODO: at this point we should be able to pass ShardingSettings
  clusterSharding.init(
    Entity(
      typeKey,
      ctx => {
        val persistenceId = typeKey.persistenceIdFrom(ctx.entityId)
        println(s">>>>>>>>>>>>>> $persistenceId")
        println(s">>>>>>>>>>>>>> ${persistenceId.id}")
        behaviorFunc(ctx).withTagger(tagger.tagFunction(persistenceId.id))
      }
    )
  )

  // TODO: need hooks to glue to projections that are based on journal sources for the entity declared here
  // When we get to this, we will be glueing write-side and read-side perfectly at model declaration
}
