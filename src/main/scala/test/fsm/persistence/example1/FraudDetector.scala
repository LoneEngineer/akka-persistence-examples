package test.fsm.persistence.example1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{ActorLogging, ActorPath, Props, Actor}

object FraudDetector {
  case class Validate(user: ActorPath, amount: Int)
  case class Accepted(user: ActorPath)
  private[FraudDetector] case class Ready(issuer: ActorPath, user: ActorPath, amount: Int)

  def props = Props(new FraudDetector)
}

class FraudDetector extends Actor with ActorLogging {
  import FraudDetector._

  override def receive = {
    case Validate(visitor, amount) =>
      log.debug(s"start validation of: ${visitor.name} for $amount ... (via ${sender().path.name})")
      context.system.scheduler.scheduleOnce(100 milliseconds, self, Ready(sender().path, visitor, amount))

    case Ready(issuer, visitor, amount) =>
      log.info(s"accepted $amount from ${visitor.name} to ${issuer.name}")
      context.system.actorSelection(issuer) ! Accepted(visitor)
  }
}
