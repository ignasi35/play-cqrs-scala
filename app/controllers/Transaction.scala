package model

import play.api.libs.json.Json

case class Transaction(amount: Double)
object Transaction {
  implicit val format = Json.format[Transaction]
}