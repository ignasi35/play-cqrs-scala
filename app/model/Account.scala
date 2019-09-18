/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package model

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.ExpectingReply
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import play.scaladsl.cqrs.Tagger
import akka.actor.typed.scaladsl.Behaviors

object Account {

  val typeKey: EntityTypeKey[AccountCommand[_]] = EntityTypeKey[AccountCommand[_]]("Account")

  private val accountTag = "account-event"

  // tagger being a val on a object can be accessed later to start projections
  val tagger =
    Tagger[AccountEvent]
      .addTagGroup(accountTag, numOfShards = 10)

  def empty: Account = Account(balance = 0)

  def behavior(entityContext: EntityContext): Behavior[AccountCommand[_]] = {
    Behaviors.setup { ctx =>
      ctx.log.info(s"Account: ${entityContext.entityId} instatiated")

      EventSourcedBehavior
        .withEnforcedReplies[AccountCommand[_], AccountEvent, Account](
          persistenceId = typeKey.persistenceIdFrom(entityContext.entityId), // AccountEntity|abc-def
          emptyState = Account.empty,
          commandHandler = (account, cmd) => {
            ctx.log.info(s"Received command $cmd")
            account.applyCommand(cmd)
          },
          eventHandler = (account, evt) => {
            ctx.log.info(s"Applying event $evt")
            account.applyEvent(evt)
          }
        )
        .withTagger(tagger.tagFunction(entityContext.entityId))
    }
  }

}

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
