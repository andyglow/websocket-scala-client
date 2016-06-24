import java.nio.charset.Charset
import com.github.andyglow.websocket.{WebsocketHandler, WebsocketClient}
import io.netty.buffer.{Unpooled, ByteBuf}
import com.github.andyglow.websocket._

object BinaryEchoWebSocketOrg extends WaitForStop {

  val protocolHandler = new WebsocketHandler[ByteBuf]() {
    def receive = {
      case bin if bin.toString(Charset.defaultCharset()) == "close" =>
        logger.info(s"<<! ${bin.toString(Charset.defaultCharset())}")
        sender().close()
        done()

      case bin =>
        logger.info(s"<<| ${bin.toString(Charset.defaultCharset())}")
    }
  }
  val control = WebsocketClient(Uri("ws://echo.websocket.org/?encoding=binary"), protocolHandler)
  val ws = control.open()

  ws ! Unpooled.wrappedBuffer("hello".getBytes)
  ws ! Unpooled.wrappedBuffer("world".getBytes)
  ws ! Unpooled.wrappedBuffer("close".getBytes)


}
