import akka.actor.ActorSystem
import akka.testkit.{TestKitBase, ImplicitSender}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import test.fsm.persistence.example1._

class Example1Test extends WordSpecLike with TestKitBase with ImplicitSender with ScalaFutures with Matchers with LazyLogging {

  lazy val appCfg = ConfigFactory.load("akka-test.conf")
  override lazy val system = ActorSystem(getClass.getSimpleName, appCfg)

  implicit val askTimeout: Timeout = 10 seconds
  implicit val pc = PatienceConfig(timeout = 35 seconds)
  val runs = 20

  "multiple visitors" in {
    val shop = system.actorOf(Shop.props(false))
    val tests = for {i <- 1 to runs} yield {
        shop ! Shop.Enter
        val cart = expectMsgPF(1 second) {
          case Shop.Cart(x) => x
        }
        logger.info(s"starting shopping with ${cart.path.name} ...")
        cart ! AddItem(Item("book.1", "Some name", 12))
        cart ! AddItem(Item("journal.1", "Another name", 3))
        (cart ? Buy).recover { case _ => false }
    }
    val codes = Future.sequence(tests).futureValue
    codes.length should be (runs)
    codes.collect{ case Receipt(amount, code) if amount == 15 => code }.length should be (runs)
  }

  "restart visitor actor" in {
    val shop = system.actorOf(Shop.props(true))
    val tests = for {i <- 1 to runs} yield {
      shop ! Shop.Enter
      val cart = expectMsgPF(1 second) {
        case Shop.Cart(x) => x
      }
      logger.info(s"starting shopping with ${cart.path.name} ...")
      cart ! AddItem(Item("book.1", "Some name", 12))
      cart ! AddItem(Item("journal.1", "Another name", 3))
      (cart ? Buy).recover { case _ => false }
    }
    val codes = Future.sequence(tests).futureValue
    codes.length should be (runs)
    codes.collect{ case Receipt(amount, code) if amount == 15 => code }.length should be < runs
  }
}
