package com.github.andyglow.websocket

import java.net.http.{WebSocket => JWebsocket}
import java.net.http.HttpClient
import java.nio.ByteBuffer
import java.util.concurrent.{CompletionStage, Executor, Executors}
import org.slf4j.LoggerFactory
import org.slf4j.MDC

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

trait JdkClient { this: JdkPlatform =>

  private class JdkWebsocketListener(handler: WebsocketHandler, executor: Executor) extends JWebsocket.Listener {

    def nonBlocking(fn: => Any): Unit = executor.execute(new Runnable {
      override def run(): Unit = {
        fn
        ()
      }
    })

    override def onPong(webSocket: JWebsocket, message: ByteBuffer): CompletionStage[_] = {
      nonBlocking {
        if (handler.onMessage.isDefinedAt(())) handler.onMessage(())
        else handler.onUnhandledMessage(())
        webSocket.request(1)
      }

      null
    }

    override def onText(webSocket: JWebsocket, data: CharSequence, last: Boolean): CompletionStage[_] = {
      nonBlocking {
        if (handler.onMessage.isDefinedAt(data)) handler.onMessage(data)
        else handler.onUnhandledMessage(data)
        webSocket.request(1)
      }

      null
    }

    override def onBinary(webSocket: JWebsocket, data: ByteBuffer, last: Boolean): CompletionStage[_] = {
      nonBlocking {
        if (handler.onMessage.isDefinedAt(data)) handler.onMessage(data)
        else handler.onUnhandledMessage(data)
        webSocket.request(1)
      }

      null
    }

    override def onClose(webSocket: JWebsocket, statusCode: Int, reason: String): CompletionStage[_] = {
      handler.onClose(())
      null
    }
  }

  private class JdkClient(
    serverAddress: ServerAddress,
    options: Options
  ) extends WebsocketClient {

    override implicit val ic: InternalContext = JdkInternalContext

    override def open(handler: WebsocketHandler): Websocket = {
      val httpBuilder = HttpClient.newBuilder()

      options.sslCtx foreach { sslCtx => httpBuilder.sslContext(sslCtx) }

      val executor = httpBuilder.build().executor().getOrElse(Executors.newCachedThreadPool())
      val wsBuilder = httpBuilder.build().newWebSocketBuilder()

      options.subProtocol foreach { x => wsBuilder.subprotocols(x) }
      options.headers foreach { case (k, v) => wsBuilder.header(k, v) }

      val jWs = wsBuilder
        .buildAsync(serverAddress.build(), new JdkWebsocketListener(handler, executor))
        .join()

      val ws =
        new JdkWebsocket(jWs, options.futureResolutionTimeout, options.tracer.lift.andThen(_ => ()), executor)
          with Websocket.TracingAsync
      handler._sender = ws
      ws
    }

    def shutdown(): Unit = ()
  }

  override def newClient(
    address: ServerAddress,
    options: Options = defaultOptions
  ): WebsocketClient = new JdkClient(address, options)

  class JdkWebsocket(
    ws: java.net.http.WebSocket,
    val timeout: FiniteDuration,
    val trace: Trace,
    executor: Executor
  ) extends Websocket
      with Websocket.AsyncImpl {

    override protected implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)

    override protected def sendAsync(x: MessageType): Future[Unit] = {
      val f = x match {
        case binary: Binary => ws.sendBinary(binary, true)
        case text: Text     => ws.sendText(text, true)
        case _: Pong        => ws.sendPong(ByteBuffer.allocate(0))
        case _              => throw new IllegalArgumentException(s"unsupported message type: ${x.getClass}")
      }
      AdaptJdkFuture(f).map(_ => ())
    }
    override protected def pingAsync(): Future[Unit] = {
      AdaptJdkFuture(ws.sendPing(ByteBuffer.allocate(0))).map(_ => ())
    }
    override protected def closeAsync(): Future[Unit] = {
      if (ws.isOutputClosed) Future.successful(())
      else AdaptJdkFuture(ws.sendClose(JWebsocket.NORMAL_CLOSURE, "ok")).map(_ => ())
    }
  }
}
