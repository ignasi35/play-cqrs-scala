import _root_.controllers.AssetsComponents
import com.softwaremill.macwire._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n._
import play.api.mvc._
import play.api.routing.Router
import router.Routes

import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.actor.typed.scaladsl.adapter._

/**
 * Application loader that wires up the application dependencies using Macwire
 */
class AccountApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = new AccountComponents(context).application
}

class AccountComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with AccountComponent
    with play.filters.HttpFiltersComponents {

  // set up logger
  LoggerConfigurator(context.environment.classLoader).foreach {
    _.configure(context.environment, context.initialConfiguration, Map.empty)
  }

  val typedActorSystem = actorSystem.toTyped
  val cluster          = Cluster(typedActorSystem)
  cluster.manager ! Join.create(cluster.selfMember.address)

  lazy val router: Router = {
    // add the prefix string in local scope for the Routes constructor
    val prefix: String = "/"
    wire[Routes]
  }
}
