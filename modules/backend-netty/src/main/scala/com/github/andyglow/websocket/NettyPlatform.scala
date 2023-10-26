package com.github.andyglow.websocket

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class NettyPlatform extends Platform with NettyImplicits with NettyClient {
  override type MessageType     = WebSocketFrame
  override type Binary          = BinaryWebSocketFrame
  override type Text            = TextWebSocketFrame
  override type Pong            = PongWebSocketFrame
  override type InternalContext = NettyInternalContext.type
  object NettyInternalContext
  override val implicits = new NettyImplicits with Implicits

  case class NettyOptions(
    override val headers: Map[String, String] = Map.empty,
    override val subProtocol: Option[String] = None,
    override val tracer: Tracer = acceptingAnyPF(()),
    logLevel: Option[io.netty.handler.logging.LogLevel] = None,
    sslCtx: Option[io.netty.handler.ssl.SslContext] = None,
    futureResolutionTimeout: FiniteDuration = NettyOptions.DefaultFutureResolutionTimeout,
    maxFramePayloadLength: Int = NettyOptions.DefaultFramePayloadLength,
    shutdownQuietPeriod: FiniteDuration = NettyOptions.DefaultShutdownQuietPeriod,
    shutdownTimeout: FiniteDuration = NettyOptions.DefaultShutdownTimeout,
    executionContext: ExecutionContext = ExecutionContext.global
  ) extends CommonOptions {
    def withTracer(tracer: Tracer): NettyOptions = {
      copy(tracer = tracer)
    }
  }

  object NettyOptions {
    private val DefaultFramePayloadLength                      = 65536
    private val DefaultFutureResolutionTimeout: FiniteDuration = 2.seconds
    private val DefaultShutdownQuietPeriod: FiniteDuration     = 2.seconds
    private val DefaultShutdownTimeout: FiniteDuration         = 5.seconds
  }

  override type Options = NettyOptions
  override def defaultOptions: Options = NettyOptions()

  override protected def stringify(x: WebSocketFrame): String = NettyStringify(x)
}

object NettyPlatform extends NettyPlatform
