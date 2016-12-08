import com.github.andyglow.websocket._

object SimplifiedTextEchoWebSocketOrg extends WaitForStop[String] {

  // 1. prepare ws-client
  // 2. define message handler
  val control = WebsocketClient[String]("ws://echo.websocket.org") {
    case "stop" =>
      logger.info(s"<<! stop")
      done()

    case str =>
      logger.info(s"<<| $str")
  }

  // 4. open websocket
  val ws = control.open()

  // 5. send messages
  ws ! "hello"
  ws ! "world"
  ws ! "stop"

}
