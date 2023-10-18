import com.github.andyglow.websocket._
import com.github.andyglow.websocket.WebsocketClient
import com.github.andyglow.websocket.WebsocketHandler
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.Charset

object BinaryEchoWebSocketOrg extends WaitForStop[ByteBuf] {

  val protocolHandler = new WebsocketHandler[ByteBuf]() {
    def onMessage = {
      case bin if bin.toString(Charset.defaultCharset()) == "close" =>
        logger.info(s"<<! ${bin.toString(Charset.defaultCharset())}")
        done()

      case bin =>
        logger.info(s"<<| ${bin.toString(Charset.defaultCharset())}")
    }
  }

  val client = WebsocketClient(binaryUri, protocolHandler)

  def run(): Unit = {
    socket ! Unpooled.wrappedBuffer("hello".getBytes)
    socket ! Unpooled.wrappedBuffer("world".getBytes)
    socket ! Unpooled.wrappedBuffer("close".getBytes)
  }
}
