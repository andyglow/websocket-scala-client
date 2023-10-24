import java.nio.charset.Charset
import com.github.andyglow.websocket.{WebsocketHandler, WebsocketClient}
import io.netty.buffer.{Unpooled, ByteBuf}
import com.github.andyglow.websocket._

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
