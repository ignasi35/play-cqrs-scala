import play.api.mvc.ControllerComponents
import akka.cluster.sharding.typed.scaladsl._
import play.scaladsl.cqrs._
import com.softwaremill.macwire._
import model._
import _root_.controllers.AccountController

trait AccountComponent extends ClusterShardingComponents with CqrsComponents {

  lazy val accountController = wire[AccountController]

  def controllerComponents: ControllerComponents

  // register behavior as a sharded entity
  clusterSharding.init(
    Entity(
      Account.typeKey,
      ctx => Account.behavior(ctx)
    )
  )

}
