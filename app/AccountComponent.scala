import play.api.mvc.ControllerComponents
import akka.cluster.sharding.typed.scaladsl._
import play.scaladsl.cqrs._
import com.softwaremill.macwire._
import model._
import controllers.AccountController

trait AccountComponent extends ClusterShardingComponents with CqrsComponents {

  lazy val accountController = wire[AccountController]

  def controllerComponents: ControllerComponents

//  createEntityFactory(
//    Account.typeKey.name,
//    Account.behavior,
//    Tagger[AccountEvent].addTagGroup("TagA", 3)
//  )

  // register behavior as a sharded entity
  clusterSharding.init(
    Entity(
      Account.typeKey,
      ctx => Account.behavior(ctx)
    )
  )

}
