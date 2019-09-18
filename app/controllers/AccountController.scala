package controllers

import play.scaladsl.cqrs._
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import scala.concurrent.Future
import model._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef

class AccountController(
    cc: ControllerComponents,
    clusterSharding: ClusterSharding
) extends AbstractController(cc) {

  implicit val timeout = Timeout(3.seconds)

  private def accountRef(accountNum: String): EntityRef[AccountCommand[_]] =
    clusterSharding.entityRefFor(Account.typeKey, accountNum)

  def balance(accountNum: String) = Action.async {

    val res = accountRef(accountNum) ? GetBalance
    res.map {
      case Balance(amount) => Ok("current balance = " + amount)
    }
  }

  def deposit(accountNum: String) = Action.async(parse.json) { req =>
    def run(value: Double) = {
      val res = accountRef(accountNum).ask(replyTo => Deposit(value, replyTo))
      res.map {
        case model.Accepted   => Ok("deposited successful")
        case Rejected(reason) => BadRequest(s"deposited rejected: $reason")
      }
    }

    Json.fromJson[Transaction](req.body) match {
      case JsSuccess(Transaction(amount), _) => run(amount)
      case err: JsError                      => Future.successful(BadRequest(JsError.toJson(err)))
    }

  }

  def withdraw(accountNum: String) = Action.async(parse.json) { req =>
    def run(value: Double) = {
      val res = accountRef(accountNum).ask(replyTo => Withdraw(value, replyTo))
      res.map {
        case model.Accepted   => Ok("withdraw successful")
        case Rejected(reason) => BadRequest(s"withdraw rejected: $reason")
      }
    }

    Json.fromJson[Transaction](req.body) match {
      case JsSuccess(Transaction(amount), _) => run(amount)
      case err: JsError                      => Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

}
