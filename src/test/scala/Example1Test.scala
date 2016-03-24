import java.util.UUID
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKitBase, ImplicitSender}
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike
import test.fsm.persistence.example1._

class Example1Test extends WordSpecLike with TestKitBase with ImplicitSender with LazyLogging {

  lazy val appCfg = ConfigFactory.load("akka-test.conf")
  override lazy val system = ActorSystem(getClass.getSimpleName, appCfg)

  val shop = system.actorOf(Shop.props)

  "restart visitor actor" in {
    logger.info("starting shopping ...")
    shop ! Shop.Enter
    val cart = expectMsgPF(1 second) {
      case Shop.Cart(x) => x
    }
    cart ! AddItem(Item("book.1", "Some name", 12))
    cart ! AddItem(Item("journal.1", "Another name", 3))
    cart ! Buy
    expectMsgPF(1 second) {
      case Receipt(amount, code) if amount == 15 =>
        logger.info(s"Successfully purchased items (amount: $amount) and get code : $code")
    }
  }
}
