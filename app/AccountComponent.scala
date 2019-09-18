import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import play.api.mvc.ControllerComponents
import akka.cluster.sharding.typed.scaladsl._
import play.scaladsl.cqrs._
import com.softwaremill.macwire._
import model._
import controllers.AccountController

trait AccountComponent extends ClusterShardingComponents with CqrsComponents {

  lazy val accountController = wire[AccountController]

  def controllerComponents: ControllerComponents

  // OPTION A:
  // This is the regular usage of the factory.
  //  - CqrsComponents#createEntityFactory isolates the user from the sharding
  //    aspects so the `behaviorFRunc` argument takes (ActorContext[], PersistenceId)
  createEntityFactory(
    Account.typeKey.name,
    Account.behavior,
    Tagger[AccountEvent].addTagGroup("TagA", 3)
  )

  // OPTION B:
  // This is how you use it when you want an extra ActorContext on your EventSourcedBehavior
  //  - ignore the `createEntityFactory()` method
  //  - create an anonymous sub-class of EntityFactory and override `defaultSharder`
  new EntityFactory(
    Account.typeKey.name + "-on-steroids",
    Account.behavior,
    Tagger[AccountEvent].addTagGroup("TagA", 3),
    actorSystem
  ) {

    // the sharder is what I call the function that a ShardRegion will use to spawn
    // a new Behavior on the receiving node. It gets an actorContext and a entityContext
    // because it is used directly from the ShardRegion.
    override def defaultSharder(
        actorContext: ActorContext[AccountCommand[_]],
        entityContext: EntityContext
    ): Behavior[AccountCommand[_]] = {
      val spr = super.defaultSharder _
      actorContext.log.info(s"Account: ${entityContext.entityId} instatiated")
      spr(actorContext, entityContext)
    }

  }

}
