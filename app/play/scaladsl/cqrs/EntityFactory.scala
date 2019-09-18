/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.scaladsl.EventSourcedBehavior

import scala.reflect.ClassTag
import akka.annotation.ApiMayChange
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId

@ApiMayChange
class EntityFactory[Command: ClassTag, Event, State](
    name: String,
    behaviorFunc: (
        ActorContext[Command],
        PersistenceId) => EventSourcedBehavior[Command, Event, State],
    tagger: Tagger[Event],
    actorSystem: ActorSystem
) {

  // Play has an injectable (untyped) ActorySystem. We can just use it.
  private val typedActorSystem = actorSystem.toTyped
  private val clusterSharding = ClusterSharding(typedActorSystem)

  private val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command](name)

  final def entityRefFor(entityId: String): EntityRef[Command] = {
    // Whatever entityId the user provides is the eid used across the sharding
    // scheme. The `typeKey` will take care of providing some namespacing to
    // the `entityId`.
    clusterSharding.entityRefFor(typeKey, entityId)
  }

  private def asPersistenceId(eid: String): PersistenceId =
    typeKey.persistenceIdFrom(eid)

  private def buildEntity(): Entity[Command, ShardingEnvelope[Command]] = {
    Entity(
      typeKey,
      entityContext =>
        Behaviors.setup[Command] { actorContext =>
          defaultSharder(actorContext, entityContext)
      }
    )
  }

  clusterSharding.init(buildEntity())

  // This is the glue code connecting sharding and persistence. But we still
  // leave the door open to producing regular `Behavior` so users can override and wrap this.
  protected def defaultSharder(
      actorContext: ActorContext[Command],
      entityContext: EntityContext): Behavior[Command] = {
    val peid = asPersistenceId(entityContext.entityId)
    behaviorFunc(actorContext, peid)
      .withTagger(
        tagger.tagFunction(peid.id)
        // IMHO this is wrong and using the peid.id
        // on tagging directly introduces a limitation
      )
  }

}
