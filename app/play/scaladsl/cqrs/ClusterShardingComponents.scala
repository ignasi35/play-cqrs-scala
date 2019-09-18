package play.scaladsl.cqrs
import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join

trait ClusterShardingComponents {

  def actorSystem: ActorSystem

  lazy val clusterSharding: ClusterSharding = {
    val typedActorSystem = actorSystem.toTyped
    val cluster = Cluster(typedActorSystem)
    cluster.manager ! Join.create(cluster.selfMember.address)
    ClusterSharding(typedActorSystem)
  }
}
