/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs
import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import akka.cluster.sharding.typed.scaladsl._
import scala.reflect.ClassTag

trait CqrsComponents extends ClusterShardingComponents {

  def clusterSharding: ClusterSharding

  final def newEntityFactory[Command: ClassTag, Event, State](
      name: String,
      behaviorFunc: EntityContext => EventSourcedBehavior[Command, Event, State],
      tagger: Tagger[Event] = new Tagger[Event]()
  ): CqrsEntityFactory[Command, Event, State] = {

    new CqrsEntityFactory[Command, Event, State](
      name = name,
      typeKey = EntityTypeKey[Command](name).withEntityIdSeparator("-"),
      behaviorFunc = behaviorFunc,
      tagger = tagger,
      clusterSharding
    )
  }

}
