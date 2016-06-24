import com.github.andyglow.websocket._

object TextEchoWebSocketOrg extends WaitForStop {

  val protocolHandler = new WebsocketHandler[String]() {
    def receive = {
      case str if str startsWith "repeat " =>
        sender() ! "repeating " + str.substring(7)
        logger.info(s"<<| $str")

      case str if str endsWith "close" =>
        logger.info(s"<<! $str")
        sender().close()
        done()

      case str =>
        logger.info(s"<<| $str")
    }
  }

  val control = WebsocketClient(Uri("ws://echo.websocket.org"), protocolHandler)
  val ws = control.open()

  ws ! "hello"
  ws ! "world"
  ws ! "repeat and close"

}
