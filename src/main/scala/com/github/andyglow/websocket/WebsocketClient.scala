package com.github.andyglow.websocket

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

import scala.concurrent.{ExecutionContext, Future}

class WebsocketClient[T : MessageFormat] private(
  uri: Uri,
  subprotocol: Option[String],
  sslCtx: Option[SslContext],
  customHeaders: HttpHeaders,
  handler: WebsocketHandler[T],
  logLevel: Option[LogLevel]) {

  private val group = new NioEventLoopGroup()

  private val clientHandler = new WebsocketNettytHandler(
    new WebsocketNettyHandshaker(uri, customHeaders, subprotocol),
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

  def open(): Websocket = {
    bootstrap.connect(uri.host, uri.port).sync()
    clientHandler.waitForHandshake()
  }

  def shutdownSync(): Unit = {
    group.shutdownGracefully().syncUninterruptibly()
  }

  def shutdownAsync(implicit ex: ExecutionContext): Future[Unit] = {
    val f = NettyFuture(group.shutdownGracefully())
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
    maybeSslCtx: Option[SslContext] = None): WebsocketClient[T] = {

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

    new WebsocketClient(uri, subprotocol, sslCtx, customHeaders, handler, logLevel)
  }
}
