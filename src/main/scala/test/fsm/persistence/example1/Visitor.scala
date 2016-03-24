package test.fsm.persistence.example1

import java.util.UUID

import akka.actor.{ActorLogging, Props, ActorRef}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import scala.concurrent.duration._
import scala.reflect._
import scala.util.Random


sealed trait Command
case class AddItem(item: Item) extends Command
case object Buy extends Command
case object Leave extends Command
case object GetCurrentCart extends Command
sealed trait Reply
case class Receipt(amount: Int, code: String) extends Reply

sealed trait UserState extends FSMState
case object LookingAround extends UserState {
  override def identifier: String = "Looking Around"
}
case object Shopping extends UserState {
  override def identifier: String = "Shopping"
}
case object Inactive extends UserState {
  override def identifier: String = "Inactive"
}
case object Validation extends UserState {
  override def identifier: String = "Validation"
}
case object Paid extends UserState {
  override def identifier: String = "Paid"
}

sealed trait DomainEvent
case class ItemAdded(item: Item) extends DomainEvent
case object OrderExecuted extends DomainEvent
case object OrderDiscarded extends DomainEvent
case object OrderPaid extends DomainEvent

sealed trait ShoppingCart {
  def addItem(item: Item): ShoppingCart
  def empty(): ShoppingCart
  def amount: Int
}
case object EmptyShoppingCart extends ShoppingCart {
  def addItem(item: Item) = NonEmptyShoppingCart(item :: Nil)
  def empty() = this
  def amount = 0
}
case class NonEmptyShoppingCart(items: Seq[Item]) extends ShoppingCart {
  def addItem(item: Item) = NonEmptyShoppingCart(items :+ item)
  def empty() = EmptyShoppingCart
  def amount = items.map(_.price).sum
}

object Visitor {
  def props(id: String, validator: ActorRef, unstable: Boolean) = Props(new Visitor(id, validator, unstable))
}

class Visitor(id: String, validator: ActorRef, unstable: Boolean) extends PersistentFSM[UserState, ShoppingCart, DomainEvent] with ActorLogging {

  override val persistenceId: String = s"${context.system.name}.Visitor.$id.v1"
  override def domainEventClassTag: ClassTag[DomainEvent] = classTag[DomainEvent]
  override def applyEvent(event: DomainEvent, cartBeforeEvent: ShoppingCart): ShoppingCart = {
    // log.info(s"-- $id : apply event($event)")
    event match {
      case ItemAdded(item) => cartBeforeEvent.addItem(item)
      case OrderDiscarded  => cartBeforeEvent.empty()
      case OrderExecuted   => cartBeforeEvent
      case OrderPaid       => cartBeforeEvent
    }
  }

  val rnd = new Random()
  def mayFail = {
    if (unstable && rnd.nextDouble() < 0.2)
      throw new IllegalStateException(s"$id eventually crashed")
  }

  startWith(LookingAround, EmptyShoppingCart)

  when(LookingAround) {
    case Event(AddItem(item), _) =>
      goto(Shopping) applying ItemAdded(item) forMax (1 seconds)
    case Event(GetCurrentCart, data) =>
      stay replying data
  }

  when(Inactive) {
    case Event(AddItem(item), _) =>
      goto(Shopping) applying ItemAdded(item) forMax (1 seconds)
    case Event(StateTimeout, _) =>
      stop applying OrderDiscarded andThen {
        case _ => log.warning(s"$id timed out")
      }
  }

  when(Shopping) {
    case Event(AddItem(item), _) =>
      stay applying ItemAdded(item) forMax (1 seconds)
    case Event(Buy, _) =>
      goto(Validation) applying OrderExecuted andThen {
        case cart =>
          mayFail // <-- this is point of unrecoverable failure for flow
          validator ! FraudDetector.Validate(sender().path, cart.amount)
      }
    case Event(Leave, _) =>
      stop applying OrderDiscarded andThen {
        case _ => log.warning(s"$id left us")
      }
    case Event(GetCurrentCart, data) =>
      stay replying data
    case Event(StateTimeout, _) =>
      goto(Inactive) forMax (2 seconds)
  }

  when(Validation) {
    case Event(FraudDetector.Accepted(from), cart) =>
      val code = UUID.randomUUID().toString
      context.system.actorSelection(from) ! Receipt(cart.amount, code)
      goto(Paid) applying OrderPaid andThen {
        case NonEmptyShoppingCart(items) => log.info(s"$id purchased ${items.mkString} (code: $code)")
        case EmptyShoppingCart           => // do nothing...
      }
    case Event(Leave, _) =>
      stop applying OrderDiscarded andThen {
        case _ => log.warning(s"$id left us")
      }
    case Event(StateTimeout, _) =>
      stop applying OrderDiscarded andThen {
        case _ => log.warning(s"$id timed out")
      }
  }
  
  when(Paid) {
    case Event(Leave, _) => stop()
    case Event(GetCurrentCart, data) =>
      stay replying data
  }

}
