import com.github.andyglow.websocket.util.Uri
import com.github.andyglow.websocket.{Websocket, WebsocketClient}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

trait WaitForStop[T] {

  // please use TestServer which could be located in `test` sources to run test ws server
  // "localhost:8080"
  val host = "echo.websocket.org"
  val binaryUri = Uri(s"ws://$host/?encoding=binary")
  val stringUri = Uri(s"ws://$host")

  val logger = LoggerFactory.getLogger("echo-websocket-example")

  private val roundtrip = Promise[Unit]()
  private val initCode = new ListBuffer[() => Unit]

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
