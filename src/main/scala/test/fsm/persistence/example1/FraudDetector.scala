package test.fsm.persistence.example1

import akka.actor.{ActorLogging, ActorPath, Props, Actor}

object FraudDetector {
  case class Validate(user: ActorPath, amount: Int)
  case class Accepted(user: ActorPath)

  def props = Props(new FraudDetector)
}

class FraudDetector extends Actor with ActorLogging {
  import FraudDetector._

  override def receive = {
    case Validate(userId, amount) =>
      log.info(s"start validation of: ${userId.name} for $amount ... (via ${sender().path.name})")
      Thread.sleep(300)
      log.info(s"accepted $amount from ${userId.name}")
      context.system.actorSelection(sender().path) ! Accepted(userId)
  }
}
