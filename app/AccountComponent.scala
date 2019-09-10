import play.api.mvc.ControllerComponents
import play.scaladsl.cqrs._
import com.softwaremill.macwire._
import model._
import _root_.controllers.AccountController

trait AccountComponent extends CqrsComponents {

  lazy val accountController = wire[AccountController]

  def controllerComponents: ControllerComponents

  private val accountTag      = "account-event"
  private val kafkaAccountTag = "account-event-ext"

  private val tagger =
    Tagger[AccountEvent]
      .addTagGroup(accountTag, numOfShards = 10)
      .addTagGroup(kafkaAccountTag, numOfShards = 5)

  lazy val accountFactory: CqrsEntityFactory[AccountCommand[_], AccountEvent, Account] =
    newEntityFactory("account", Account.behavior, tagger)

}
