package scalax.websocket

import java.net.URL

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContext, SslContextBuilder}

class WebSocketClient private(
    url: URL,
    sslCtx: Option[SslContext],
    customHeaders: HttpHeaders,
    listener: WebSocketMessageListener,
    logLevel: Option[LogLevel]) {

  private val group = new NioEventLoopGroup()

  private val handler = new WebSocketClientHandler(
    new WebSocketClientHandshaker(url, customHeaders),
    listener)

  private val bootstrap = {
    val b = new Bootstrap()
    b.group(group)
      .channel(classOf[NioSocketChannel])
      .handler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel) {
          val p = ch.pipeline()
          for (sslCtx <- sslCtx) p.addLast(sslCtx.newHandler(ch.alloc(), url.getHost, url.getPort))
          logLevel foreach (level => p.addLast(new LoggingHandler(level)))
          p.addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(8192),
            handler)
        }
      })
  }

  def open(): WebSocket = {
    val connect = bootstrap.connect(url.getHost, url.getPort)
    val channel = connect.sync().channel()
    handler.waitForHandshake()
    new WebSocket(channel)
  }

  def close() = group.shutdownGracefully()

}

object WebSocketClient {

  val defaultPorts: Map[String, Int] = Map("ws" -> 80, "wss" -> 443)

  def apply(
    url: URL,
    listener: WebSocketMessageListener,
    customHeaders: HttpHeaders = new DefaultHttpHeaders(),
    logLevel: Option[LogLevel] = None): WebSocketClient = {

      require(url.getProtocol == "ws" || url.getProtocol == "wss")
      val host = Option(url.getHost) getOrElse "127.0.0.1"
      val port = if (url.getPort != 0) url.getPort else defaultPorts(url.getProtocol)
      val sslCtx = if (url.getProtocol == "wss") Some {
        SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
      } else
        None

      new WebSocketClient(url, sslCtx, customHeaders, listener, logLevel)
    }
  }
