package test.fsm.persistence.example1

import java.util.UUID
import akka.actor._
import scala.util.control._

object Shop {
  def props = Props(new Shop)

  case object Enter
  case class Cart(actor: ActorRef)
}
class Shop extends Actor with ActorLogging {
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy()(SupervisorStrategy.defaultDecider.orElse {
    case NonFatal(_) => SupervisorStrategy.Restart
  })

  lazy val validator = context.actorOf(FraudDetector.props)

  override def receive = running(Set.empty)

  def running(carts: Set[ActorRef]): Receive = {
    case Shop.Enter =>
      val id = UUID.randomUUID().toString
      val actor = start(id)
      log.debug(s"started new cart ${actor.path.name}")
      sender() ! Shop.Cart(actor)
      context.become(running(carts + actor))

    case Terminated(actor) if carts.contains(actor) =>
      log.debug(s"cart ${actor.path.name} is finished ...")
      context.become(running(carts - actor))
  }

  private def start(id: String): ActorRef =  {
    val actor = context.actorOf(Visitor.props(id, validator), s"visitor.$id")
    context.watch(actor)
    actor
  }
}
