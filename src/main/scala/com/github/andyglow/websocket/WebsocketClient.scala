package com.github.andyglow.websocket

import java.util.concurrent.TimeUnit

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContext, SslContextBuilder}

import scala.concurrent.duration.FiniteDuration
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
    new WebsocketNettyHandshaker(uri, customHeaders, subprotocol, maxFramePayloadLength),
    handler)

  private val bootstrap = {
    val b = new Bootstrap()
    b.group(group)
      .channel(classOf[NioSocketChannel])
      .handler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel) {
          val p = ch.pipeline()
          for (sslCtx <- sslCtx) p.addLast(sslCtx.newHandler(ch.alloc(), uri.host, uri.port))
          logLevel foreach (level => p.addLast(new LoggingHandler(level)))
          p.addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(8192),
            new WebSocketFrameAggregator(Integer.MAX_VALUE),
            clientHandler)
        }
      })
  }

  private def shutdown() =
    group.shutdownGracefully(shutdownQuietPeriod.toMillis, shutdownTimeout.toMillis, TimeUnit.MILLISECONDS)

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

  def apply[T : MessageFormat](uri: String)(receive: PartialFunction[T, Unit]): WebsocketClient[T] = apply(Uri(uri), WebsocketHandler(receive))
  def apply[T : MessageFormat](uri: Uri)(receive: PartialFunction[T, Unit]): WebsocketClient[T] = apply(uri, WebsocketHandler(receive))

  def apply[T : MessageFormat](
    uri: Uri,
    handler: WebsocketHandler[T],
    headers: Map[String, String] = Map.empty,
    logLevel: Option[LogLevel] = None,
    subprotocol: Option[String] = None,
    maybeSslCtx: Option[SslContext] = None,
    maxFramePayloadLength: Int = 65536,
    shutdownQuietPeriod: FiniteDuration = FiniteDuration(2, TimeUnit.SECONDS),
    shutdownTimeout: FiniteDuration = FiniteDuration(15, TimeUnit.SECONDS)): WebsocketClient[T] = {

    require(shutdownTimeout >= shutdownQuietPeriod, "shutdownTimeout should be >= shutdownQuietPeriod")

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
      shutdownTimeout
    )
  }
}
