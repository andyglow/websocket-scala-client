import java.util.concurrent.Semaphore

import com.github.andyglow.websocket.{Websocket, WebsocketClient}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

trait WaitForStop[T] extends DelayedInit {

  val logger = LoggerFactory.getLogger("echo-websocket-example")

  private val roundtrip = Promise[Unit]()
  private val initCode = new ListBuffer[() => Unit]

  def control: WebsocketClient[T]
  def ws: Websocket

  override def delayedInit(body: => Unit): Unit = {
    initCode += (() => body)
  }

  def done(): Unit = {
    ws.close andThen {
      case _ => roundtrip success ()
    }
  }

  def main(args: Array[String]): Unit = {
    for (proc <- initCode) proc()

    val f = roundtrip.future flatMap (_ => control.shutdownAsync) andThen {
      case Success(_) => logger.info("!!! [closed]")
      case Failure(x) => logger.error("!!! [failure on close]", x)
    }

    Await.result(f, 5000.millis)
  }

}
