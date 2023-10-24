import com.github.andyglow.websocket.util.ServerAddressBuilder
import com.github.andyglow.websocket.{Websocket, WebsocketClient}
import org.slf4j.LoggerFactory

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

trait WaitForStop[T] {

  val host = "localhost:8080"
  val binaryUri = ServerAddressBuilder(s"ws://$host/?encoding=binary")
  val stringUri = ServerAddressBuilder(s"ws://$host")

  val logger = LoggerFactory.getLogger("echo-websocket-example")

  private val roundtrip = Promise[Unit]()

  def client: WebsocketClient[T]
  lazy val socket: Websocket = client.open()

  def run(): Unit

  def done(): Unit = {
    socket.close andThen {
      case _ => roundtrip.success(())
    }
  }

  def main(args: Array[String]): Unit = {
    run()

    val f = roundtrip.future flatMap (_ => client.shutdownAsync) andThen {
      case Success(_) => logger.info("!!! [closed]")
      case Failure(x) => logger.error("!!! [failure on close]", x)
    }

    Await.result(f, 5000.millis)
  }
}
