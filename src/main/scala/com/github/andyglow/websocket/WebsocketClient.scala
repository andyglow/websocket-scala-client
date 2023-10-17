package com.github.andyglow.websocket

import com.github.andyglow.websocket.util.NettyFuture
import com.github.andyglow.websocket.util.ServerAddressBuilder
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufHolder
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

/** Entrypoint class. In order to obtain websocket you should first create a client, which is configured using several
  * main parts.
  *   - Network: server address, protocols, ssl, headers, timeouts, etc.
  *   - And Handler. A way to handle server-to-client messages.
  *
  * @param serverAddress
  *   Server Address
  * @param subprotocol
  *   SubProtocol
  * @param sslCtx
  *   SSL Context
  * @param customHeaders
  *   Custom Headers if any
  * @param handler
  *   Server Message Handler
  * @param logLevel
  *   Log Level
  * @param maxFramePayloadLength
  *   Max size for the Single Frame
  * @param shutdownQuietPeriod
  *   Shutdown period
  * @param shutdownTimeout
  *   Shutdown timeout
  * @tparam T
  *   Message Type
  */
class WebsocketClient[T: MessageAdapter] private (
  serverAddress: ServerAddressBuilder,
  subprotocol: Option[String],
  sslCtx: Option[SslContext],
  customHeaders: HttpHeaders,
  handler: WebsocketHandler[T],
  logLevel: Option[LogLevel],
  maxFramePayloadLength: Int,
  shutdownQuietPeriod: FiniteDuration,
  shutdownTimeout: FiniteDuration
) {

  private val nettyLoopGroup = new NioEventLoopGroup()

  private val nettyHandler = new WebsocketNettyHandler(
    handshaker = WebSocketClientHandshakerFactory.newHandshaker(
      serverAddress.build(),
      WebSocketVersion.V13,
      subprotocol.orNull,
      false,
      customHeaders,
      maxFramePayloadLength
    ),
    handler = handler
  )

  private val nettyBootstrap = {

    def initializer = new ChannelInitializer[SocketChannel]() {

      override def initChannel(ch: SocketChannel): Unit = {
        val p = ch.pipeline()

        for { sslCtx <- sslCtx } p.addLast(sslCtx.newHandler(ch.alloc(), serverAddress.host, serverAddress.port))

        logLevel foreach { logLevel => p.addLast(new LoggingHandler(logLevel)) }

        p.addLast(
          new HttpClientCodec(),
          new HttpObjectAggregator(Int.MaxValue),
          new websocketx.WebSocketFrameAggregator(Int.MaxValue),
          nettyHandler
        )
      }
    }

    new Bootstrap()
      .group(nettyLoopGroup)
      .channel(classOf[NioSocketChannel])
      .handler(initializer)
  }

  private def shutdown() =
    nettyLoopGroup.shutdownGracefully(shutdownQuietPeriod.toMillis, shutdownTimeout.toMillis, TimeUnit.MILLISECONDS)

  /** Opens a new websocket
    *
    * @return
    *   newly acquired WebSocket instance
    */
  def open(): Websocket = {
    nettyBootstrap.connect(serverAddress.host, serverAddress.port).sync()
    nettyHandler.blockUntilHandshaken()
  }

  /** Runs though shutdown process synchronously
    */
  def shutdownSync(): Unit = shutdown().syncUninterruptibly()

  /** Executes shutdown returning Future that is going to be resolved once shutdown process is completed or if error has
    * happened.
    *
    * @param ex
    *   Execution Context
    * @return
    *   Future
    */
  def shutdownAsync(implicit ex: ExecutionContext): Future[Unit] = {
    val f = NettyFuture(shutdown())
    f map { _ => () }
  }

}

object WebsocketClient {

  private val DefaultFramePayloadLength = 65536

  private val DefaultShutdownQuietPeriod: FiniteDuration = 2.seconds

  private val DefaultShutdownTimeout: FiniteDuration = 13.seconds

  /** Alternative constructor that requires you to only pass a server address and define a message handler.
    *
    * @param serverAddress
    *   Server Address
    * @param onMessage
    *   Server Message Handler
    * @tparam T
    *   Message Type
    * @return
    */
  def apply[T: MessageAdapter](serverAddress: String)(onMessage: PartialFunction[T, Unit]): WebsocketClient[T] =
    apply(ServerAddressBuilder(serverAddress), WebsocketHandler(onMessage))

  /** Websocket Client Builder.
    *
    * @param serverAddress
    *   Server Address
    * @param onMessage
    *   Server Message Handler
    * @param options
    *   Websocket Options
    * @tparam T
    *   type of Message
    */
  case class Builder[T: MessageAdapter](
    serverAddress: ServerAddressBuilder,
    onMessage: PartialFunction[T, Unit],
    options: Builder.Options[T] = Builder.Options()
  ) {

    /** Register Exception Handler
      *
      * @param x
      *   Exception Handler
      * @return
      */
    def onFailure(x: PartialFunction[Throwable, Unit]): Builder[T] = copy(options = options.copy(onFailure = x))

    /** Register Handler for Unhandled Messages
      *
      * @param x
      *   Handler
      * @return
      */
    def onUnhandledMessage(x: PartialFunction[T, Unit]): Builder[T] =
      copy(options = options.copy(onUnhandledMessage = x))

    /** Register Handler for Unhandled Netty Frames
      *
      * @param x
      *   Handler
      * @return
      */
    def onUnhandledFrame(x: PartialFunction[ByteBufHolder, Unit]): Builder[T] =
      copy(options = options.copy(onUnhandledFrame = x))

    /** Register Close Handler
      *
      * @param x
      *   Handler
      * @return
      */
    def onClose(x: => Unit): Builder[T] = copy(options = options.copy(onClose = _ => x))

    def build(): WebsocketClient[T] = {

      WebsocketClient.apply(
        serverAddress,
        handler = WebsocketHandler(
          onMessage,
          options.onFailure,
          options.onUnhandledMessage,
          options.onUnhandledFrame,
          options.onClose
        ),
        options.headers,
        options.logLevel,
        options.subProtocol,
        options.sslCtx,
        options.maxFramePayloadLength,
        options.shutdownQuietPeriod,
        options.shutdownTimeout
      )
    }
  }

  object Builder {

    /** Options used to build a WebSocket Client
      *
      * @param onFailure
      *   Exception Handler
      * @param onUnhandledMessage
      *   Unhandled Message Handler. If your onMessage partial function doesn't have corresponding matcher defined, the
      *   message gets propagated to unhandled messages level and gets handled by this function.
      * @param onUnhandledFrame
      *   Unhandled Frame Handler. If your MessageAdapter maps you to Text Frames, you can't handle binary frames via
      *   onMessage concept. But you can use `onUnhandledFrame` to still get access to those type of messages.
      * @param onClose
      *   Callback that is going to be invoked when websocket os closed
      * @param headers
      *   Http Headers that are going to be passed to server ws endpoint at the handshake phase
      * @param logLevel
      *   Logging Level
      * @param subProtocol
      *   Sub-Protocol
      * @param sslCtx
      *   SSL Context
      * @param maxFramePayloadLength
      *   Max Frame Size
      * @param shutdownQuietPeriod
      *   Shutdown period
      * @param shutdownTimeout
      *   Shutdown Timeout
      */
    case class Options[-T](
      onFailure: PartialFunction[Throwable, Unit] = PartialFunction.empty,
      onUnhandledMessage: Function[T, Unit] = (_: T) => (),
      onUnhandledFrame: Function[ByteBufHolder, Unit] = _ => (),
      onClose: Unit => Unit = identity,
      headers: Map[String, String] = Map.empty,
      logLevel: Option[LogLevel] = None,
      subProtocol: Option[String] = None,
      sslCtx: Option[SslContext] = None,
      maxFramePayloadLength: Int = DefaultFramePayloadLength,
      shutdownQuietPeriod: FiniteDuration = DefaultShutdownQuietPeriod,
      shutdownTimeout: FiniteDuration = DefaultShutdownTimeout
    )

    /** Creates Builder off of Server Address and a Server Message Handler.
      *
      * @param serverAddress
      *   Server Address
      * @param onMessage
      *   Server Message Handler
      * @tparam T
      *   Message Type
      * @return
      *   Web Socket Builder
      */
    def apply[T: MessageAdapter](serverAddress: String)(onMessage: PartialFunction[T, Unit]): Builder[T] =
      new Builder[T](
        serverAddress = ServerAddressBuilder(serverAddress),
        onMessage = onMessage
      )

    /** Creates Builder off of Server Address and a Server Message Handler.
      *
      * @param serverAddress
      *   Server Address Builder
      * @param onMessage
      *   Server Message Handler
      * @tparam T
      *   Message Type
      * @return
      *   Web Socket Builder
      */
    def apply[T: MessageAdapter](serverAddress: ServerAddressBuilder)(onMessage: PartialFunction[T, Unit]): Builder[T] =
      new Builder[T](serverAddress, onMessage)
  }

  /** Creates WebSocket Client off of Server Address and a Server Message Handler.
    *
    * @param serverAddress
    *   Server Address
    * @param onMessage
    *   Server Message Handler
    * @tparam T
    *   Message Type
    * @return
    *   Web Socket Builder
    */
  def apply[T: MessageAdapter](serverAddress: ServerAddressBuilder)(
    onMessage: PartialFunction[T, Unit]
  ): WebsocketClient[T] = apply(serverAddress, WebsocketHandler(onMessage))

  /** Creates WebSocket Client instance
    *
    * @param serverAddress
    *   Server Address Builder
    * @param handler
    *   Websocket Handler
    * @param headers
    *   Http Headers
    * @param logLevel
    *   Logging Level
    * @param subprotocol
    *   Sub-Protocol
    * @param sslCtxOpt
    *   SSL Context
    * @param maxFramePayloadLength
    *   Max Frame Size
    * @param shutdownQuietPeriod
    *   Shutdown Period
    * @param shutdownTimeout
    *   Shutdown Timeout
    * @tparam T
    *   Message Type
    * @return
    */
  def apply[T: MessageAdapter](
    serverAddress: ServerAddressBuilder,
    handler: WebsocketHandler[T],
    headers: Map[String, String] = Map.empty,
    logLevel: Option[LogLevel] = None,
    subprotocol: Option[String] = None,
    sslCtxOpt: Option[SslContext] = None,
    maxFramePayloadLength: Int = DefaultFramePayloadLength,
    shutdownQuietPeriod: FiniteDuration = DefaultShutdownQuietPeriod,
    shutdownTimeout: FiniteDuration = DefaultShutdownTimeout
  ): WebsocketClient[T] = {

    require(shutdownTimeout >= shutdownQuietPeriod, "It is required that shutdownTimeout is >= shutdownQuietPeriod")

    val sslCtx = if (serverAddress.secure) sslCtxOpt orElse Some {
      SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
    }
    else
      None

    val customHeaders =
      headers.foldLeft(new DefaultHttpHeaders().asInstanceOf[HttpHeaders]) { case (headers, (k, v)) =>
        headers.add(k, v)
      }

    new WebsocketClient(
      serverAddress,
      subprotocol,
      sslCtx,
      customHeaders,
      handler,
      logLevel,
      maxFramePayloadLength,
      shutdownQuietPeriod,
      shutdownTimeout
    )
  }
}
