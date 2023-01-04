import com.github.andyglow.websocket._

object SimplifiedTextEchoWebSocketOrg extends WaitForStop[String] {

  val client = {
    val builder = WebsocketClient.Builder[String](stringUri) {
      case "stop"         => logger.info(s"<<! stop"); done()
      case str            => logger.info(s"<<| $str")
    } onFailure {
      case ex: Throwable  => logger.error(s"Error occurred.", ex)
    } onClose {
      logger.info(s"<<! connection closed"); done()
    }

    builder.build()
  }

  def run(): Unit = {
    socket ! "hello"
    socket ! "world"
    socket ! "close-connection"
    socket ! "stop"
  }
}
