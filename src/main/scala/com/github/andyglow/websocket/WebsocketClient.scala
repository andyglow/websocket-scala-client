package com.github.andyglow.websocket

import java.util.concurrent.TimeUnit

import com.github.andyglow.websocket.util.{NettyFuture, Uri}
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.{websocketx, _}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContext, SslContextBuilder}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class WebsocketClient[T : MessageFormat] private(
  uri: Uri,
  subprotocol: Option[String],
  sslCtx: Option[SslContext],
  customHeaders: HttpHeaders,
  handler: WebsocketHandler[T],
  logLevel: Option[LogLevel],
  maxFramePayloadLength: Int,
  shutdownQuietPeriod: FiniteDuration,
  shutdownTimeout: FiniteDuration) {

  private val group = new NioEventLoopGroup()

  private val clientHandler = new WebsocketNettytHandler(
    handshaker = new WebsocketNettyHandshaker(uri, customHeaders, subprotocol, maxFramePayloadLength),
    handler = handler)

  private val bootstrap = {
    def initializer = new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel) {
        val p = ch.pipeline()

        for {sslCtx <- sslCtx} p.addLast(sslCtx.newHandler(ch.alloc(), uri.host, uri.port))

        logLevel foreach {logLevel => p.addLast(new LoggingHandler(logLevel))}

        p.addLast(
          new HttpClientCodec(),
          new HttpObjectAggregator(8192),
          new websocketx.WebSocketFrameAggregator(Int.MaxValue),
          clientHandler)
      }
    }

    new Bootstrap()
      .group(group)
      .channel(classOf[NioSocketChannel])
      .handler(initializer)
  }

  private def shutdown() =
    group.shutdownGracefully(
      shutdownQuietPeriod.toMillis,
      shutdownTimeout.toMillis,
      TimeUnit.MILLISECONDS)

  def open(): Websocket = {
    bootstrap.connect(uri.host, uri.port).sync()
    clientHandler.waitForHandshake()
  }

  def shutdownSync(): Unit = shutdown().syncUninterruptibly()

  def shutdownAsync(implicit ex: ExecutionContext): Future[Unit] = {
    val f = NettyFuture(shutdown())
    f map {_ => ()}
  }

}

object WebsocketClient {

  val defaultFramePayloadLength = 65536
  val defaultShutdownQuietPeriod: FiniteDuration = 2.seconds
  val defaultShutdownTimeout: FiniteDuration = 13.seconds

  def apply[T : MessageFormat](uri: String)(receive: PartialFunction[T, Unit]): WebsocketClient[T] = apply(Uri(uri), WebsocketHandler(receive))

  def apply[T : MessageFormat](uri: Uri)(receive: PartialFunction[T, Unit]): WebsocketClient[T] = apply(uri, WebsocketHandler(receive))

  case class Builder[T : MessageFormat](
    uri: Uri,
    receive: PartialFunction[T, Unit],
    options: Builder.Options = Builder.Options()) {

    def onFailure(x: PartialFunction[Throwable, Unit]): Builder[T] = copy(options = options.copy(exceptionHandler = x))

    def onClose(x: => Unit): Builder[T] = copy(options = options.copy(closeHandler = _ => x))

    def build(): WebsocketClient[T] = {
      WebsocketClient.apply(
        uri,
        handler = WebsocketHandler(receive, options.exceptionHandler, options.closeHandler),
        options.headers,
        options.logLevel,
        options.subprotocol,
        options.maybeSslCtx,
        options.maxFramePayloadLength,
        options.shutdownQuietPeriod,
        options.shutdownTimeout)
    }
  }

  object Builder {
    case class Options(
      exceptionHandler: PartialFunction[Throwable, Unit] = PartialFunction.empty,
      closeHandler: Unit => Unit = identity,
      headers: Map[String, String] = Map.empty,
      logLevel: Option[LogLevel] = None,
      subprotocol: Option[String] = None,
      maybeSslCtx: Option[SslContext] = None,
      maxFramePayloadLength: Int = defaultFramePayloadLength,
      shutdownQuietPeriod: FiniteDuration = defaultShutdownQuietPeriod,
      shutdownTimeout: FiniteDuration = defaultShutdownTimeout)

    def apply[T : MessageFormat](uri: String)(receive: PartialFunction[T, Unit]): Builder[T] = new Builder(Uri(uri), receive)
    def apply[T : MessageFormat](uri: Uri)(receive: PartialFunction[T, Unit]): Builder[T] = new Builder(uri, receive)
  }

  def apply[T : MessageFormat](
    uri: Uri,
    handler: WebsocketHandler[T],
    headers: Map[String, String] = Map.empty,
    logLevel: Option[LogLevel] = None,
    subprotocol: Option[String] = None,
    maybeSslCtx: Option[SslContext] = None,
    maxFramePayloadLength: Int = defaultFramePayloadLength,
    shutdownQuietPeriod: FiniteDuration = defaultShutdownQuietPeriod,
    shutdownTimeout: FiniteDuration = defaultShutdownTimeout): WebsocketClient[T] = {

    require(shutdownTimeout >= shutdownQuietPeriod, "It is required that shutdownTimeout is >= shutdownQuietPeriod")

    val sslCtx = if (uri.secure) maybeSslCtx.orElse {
      Some {
        SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
      }
    } else
      None

    val customHeaders =
      headers.foldLeft(new DefaultHttpHeaders().asInstanceOf[HttpHeaders]) {
        case (headers, (k, v)) => headers.add(k, v)
      }

    new WebsocketClient(
      uri,
      subprotocol,
      sslCtx,
      customHeaders,
      handler,
      logLevel,
      maxFramePayloadLength,
      shutdownQuietPeriod,
      shutdownTimeout)
  }
}
