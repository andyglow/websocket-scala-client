import com.github.andyglow.websocket._

object TextEchoWebSocketOrg extends WaitForStop[String] {

  val protocolHandler = new WebsocketHandler[String]() {
    def onMessage = {
      case str if str startsWith "repeat " =>
        sender() ! "repeating " + str.substring(7)
        logger.info(s"<<| $str")

      case str if str endsWith "close" =>
        logger.info(s"<<! $str")
        done()

      case str =>
        logger.info(s"<<| $str")
    }
  }

  val client = WebsocketClient(stringUri, protocolHandler)

  def run(): Unit = {
    socket ! "hello"
    socket ! "world"
    socket ! "repeat and close"
  }
}
