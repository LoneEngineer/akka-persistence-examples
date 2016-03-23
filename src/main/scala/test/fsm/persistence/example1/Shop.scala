package test.fsm.persistence.example1

import java.util.UUID

import akka.actor._

import scala.util.control.NonFatal

object Shop {
  def props = Props(new Shop)

  case object Enter
  case class Restart(actor: ActorRef)

  case class Cart(actor: ActorRef)
}
class Shop extends Actor with ActorLogging {
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy()(SupervisorStrategy.defaultDecider.orElse {
    case NonFatal(_) => SupervisorStrategy.Restart
  })

  lazy val validator = context.actorOf(FraudDetector.props)

  override def receive = running(Map.empty)

  def running(carts: Map[ActorRef, String]): Receive = {
    case Shop.Enter =>
      val id = UUID.randomUUID().toString
      context.become(running(carts + start(id)))

    case Shop.Restart(actor) if carts.contains(actor) =>
      val id = carts(actor)
      Thread.sleep(100) // to ensure 1st message processed
      log.info(s"Emulate restart of visitor-actor: ${actor.path.name} ...")
      context.system.stop(actor) // emulate actor's node restart
      Thread.sleep(100)
      context.become(running(carts + start(id)))

    case Terminated(actor) =>
      context.become(running(carts - actor))
  }

  def start(id: String) = {
    val actor = context.actorOf(Visitor.props(id, validator), s"visitor.$id")
    sender() ! Shop.Cart(actor)
    context.watch(actor)
    actor -> id
  }
}
