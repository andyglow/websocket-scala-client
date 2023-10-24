package com.github.andyglow.websocket

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import scala.concurrent.duration._

class NettyPlatform extends Platform with NettyImplicits with NettyClient {
  override type MessageType = WebSocketFrame
  override type Binary      = BinaryWebSocketFrame
  override type Text        = TextWebSocketFrame
  override type Pong        = PongWebSocketFrame

  case class NettyOptions(
    override val headers: Map[String, String] = Map.empty,
    override val subProtocol: Option[String] = None,
    logLevel: Option[io.netty.handler.logging.LogLevel] = None,
    sslCtx: Option[io.netty.handler.ssl.SslContext] = None,
    maxFramePayloadLength: Int = NettyOptions.DefaultFramePayloadLength,
    shutdownQuietPeriod: FiniteDuration = NettyOptions.DefaultShutdownQuietPeriod,
    shutdownTimeout: FiniteDuration = NettyOptions.DefaultShutdownTimeout
  ) extends CommonOptions

  object NettyOptions {
    private val DefaultFramePayloadLength                  = 65536
    private val DefaultShutdownQuietPeriod: FiniteDuration = 2.seconds
    private val DefaultShutdownTimeout: FiniteDuration     = 13.seconds
  }

  override type Options = NettyOptions
  override def defaultOptions: Options = NettyOptions()
}

object NettyPlatform extends NettyPlatform
