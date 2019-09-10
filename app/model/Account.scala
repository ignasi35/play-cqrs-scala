/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package model

import akka.persistence.typed.ExpectingReply
import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.journal.Tagged
import akka.persistence.typed.ExpectingReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect

/**
 * The current state held by the persistent entity.
 */
case class Account(balance: Double) {

  def applyCommand(cmd: AccountCommand[_]): ReplyEffect[AccountEvent, Account] =
    cmd match {
      case deposit @ Deposit(amount, _) =>
        Effect
          .persist(Deposited(amount))
          .thenReply(deposit) { _ =>
            Accepted
          }

      case withdraw @ Withdraw(amount, _) if balance - amount < 0 =>
        Effect.reply(withdraw)(Rejected("Insufficient balance!"))

      case withdraw @ Withdraw(amount, _) =>
        Effect
          .persist(Withdrawn(amount))
          .thenReply(withdraw) { _ =>
            Accepted
          }

      case getBalance: GetBalance =>
        Effect.none
          .thenReply(getBalance) { _ =>
            Balance(balance)
          }
    }

  def applyEvent(evt: AccountEvent): Account = {
    evt match {
      case Deposited(amount) => copy(balance = balance + amount)
      case Withdrawn(amount) => copy(balance = balance - amount)
    }
  }

}

object Account {

  def empty: Account = Account(balance = 0)

  def behavior(entityContext: EntityContext) =
    EventSourcedBehavior
      .withEnforcedReplies[AccountCommand[_], AccountEvent, Account](
        persistenceId = PersistenceId(entityContext.entityId),
        emptyState = Account.empty,
        commandHandler = (account, cmd) => account.applyCommand(cmd),
        eventHandler = (account, evt) => account.applyEvent(evt)
      )
}

sealed trait AccountEvent
case class Deposited(amount: Double) extends AccountEvent
case class Withdrawn(amount: Double) extends AccountEvent

sealed trait AccountReply
sealed trait Confirmation           extends AccountReply
case object Accepted                extends Confirmation
case class Rejected(reason: String) extends Confirmation

case class Balance(amount: Double) extends AccountReply

sealed trait AccountCommand[R <: AccountReply] extends ExpectingReply[R]

case class GetBalance(replyTo: ActorRef[Balance]) extends AccountCommand[Balance]

case class Deposit(amount: Double, replyTo: ActorRef[Confirmation]) extends AccountCommand[Confirmation]

case class Withdraw(amount: Double, replyTo: ActorRef[Confirmation]) extends AccountCommand[Confirmation]
