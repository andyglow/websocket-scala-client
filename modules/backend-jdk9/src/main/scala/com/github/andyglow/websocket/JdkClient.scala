package com.github.andyglow.websocket

import java.net.http.{WebSocket => JWebsocket}
import java.net.http.HttpClient
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random

trait JdkClient { this: JdkPlatform =>

  private class WebsocketListenerImpl(handler: WebsocketHandler) extends JWebsocket.Listener {

    override def onPong(webSocket: JWebsocket, message: ByteBuffer): CompletionStage[_] = {
      if (handler.onMessage.isDefinedAt(())) handler.onMessage(())
      else handler.onUnhandledMessage(())

      super.onPong(webSocket, message)
    }

    override def onText(webSocket: JWebsocket, data: CharSequence, last: Boolean): CompletionStage[_] = {
      if (handler.onMessage.isDefinedAt(data)) handler.onMessage(data)
      else handler.onUnhandledMessage(data)

      super.onText(webSocket, data, last)
    }

    override def onBinary(webSocket: JWebsocket, data: ByteBuffer, last: Boolean): CompletionStage[_] = {
      if (handler.onMessage.isDefinedAt(data)) handler.onMessage(data)
      else handler.onUnhandledMessage(data)

      super.onBinary(webSocket, data, last)
    }

    override def onClose(webSocket: JWebsocket, statusCode: Int, reason: String): CompletionStage[_] = {
      handler.onClose(())
      super.onClose(webSocket, statusCode, reason)
    }
  }

  private class JdkClient(
    serverAddress: ServerAddress,
    options: Options
  ) extends WebsocketClient {

    override lazy val implicits = new JdkImplcits with Implicits

    override def open(handler: WebsocketHandler): Websocket = {
      val httpBuilder = HttpClient.newBuilder()

      options.sslCtx foreach { sslCtx => httpBuilder.sslContext(sslCtx) }

      val wsBuilder = httpBuilder.build().newWebSocketBuilder()

      options.subProtocol foreach { x => wsBuilder.subprotocols(x) }
      options.headers foreach { case (k, v) => wsBuilder.header(k, v) }

      val jWs = wsBuilder
        .buildAsync(serverAddress.build(), new WebsocketListenerImpl(handler))
        .join()

      val ws = new WebsocketImpl(jWs)
      handler._sender = ws
      ws
    }

    def shutdownSync(): Unit = ()

    def shutdownAsync(implicit ex: ExecutionContext): Future[Unit] = Future.successful(())
  }

  override def newClient(
    address: ServerAddress,
    options: Options = defaultOptions
  ): WebsocketClient = new JdkClient(address, options)

  class WebsocketImpl(ws: java.net.http.WebSocket) extends Websocket {

    private trait Tracer { def log(msg: String): Unit }
    private object Trace {
      private val rnd    = new Random
      private val logger = LoggerFactory.getLogger("websocket-impl")
      def next(): Tracer = new Tracer {
        private val id = rnd.alphanumeric.take(4).mkString
        def log(msg: String): Unit = {
          try {
            MDC.put("traceId", id)
            logger.debug(msg)
          } finally {
            MDC.clear()
          }
        }
      }
    }

    override protected def send(x: MessageType): Unit = {
      val trace = Trace.next()
      trace.log("sending")
      val f = x match {
        case binary: Binary => ws.sendBinary(binary, true)
        case text: Text     => ws.sendText(text, true)
        case _: Pong        => ws.sendPong(ByteBuffer.allocate(0))
        case _              => throw new IllegalArgumentException(s"unsupported message type: ${x.getClass}")
      }
      f.thenRun { () => trace.log("sent") }
      ()
    }
    override def ping(): Unit = {
      val trace = Trace.next()
      trace.log("pinging")
      ws.sendPing(ByteBuffer.allocate(0)).thenRun { () => trace.log("pinged") }
      ()
    }
    override def close()(implicit ec: ExecutionContext): Future[Unit] = {
      val trace = Trace.next()
      trace.log("closing")
      AdaptJdkFuture {
        ws.sendClose(JWebsocket.NORMAL_CLOSURE, "ok").thenApply { ws => trace.log("closed"); () }
      }
    }
  }
}
