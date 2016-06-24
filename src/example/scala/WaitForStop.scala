import java.util.concurrent.Semaphore
import com.github.andyglow.websocket.WebsocketClientControl
import org.slf4j.LoggerFactory
import scala.collection.mutable.ListBuffer

trait WaitForStop extends DelayedInit {

  val logger = LoggerFactory.getLogger("echo-websocket-example")

  private val stopWaiter = new Semaphore(0)
  private val initCode = new ListBuffer[() => Unit]

  def control: WebsocketClientControl

  override def delayedInit(body: => Unit) = {
    initCode += (() => body)
  }

  def done() = stopWaiter.release()

  def main(args: Array[String]) = {
    for (proc <- initCode) proc()
    stopWaiter.acquire(1)
    control.shutdown()
  }

}
