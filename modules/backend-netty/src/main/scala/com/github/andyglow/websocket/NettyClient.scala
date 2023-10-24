package com.github.andyglow.websocket

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufHolder
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx._
import io.netty.handler.logging.LoggingHandler
import io.netty.util
import io.netty.util.CharsetUtil
import io.netty.util.concurrent.GenericFutureListener
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.stm._
import scala.util.Random
import scala.util.control.NonFatal

trait NettyClient { this: NettyPlatform =>

  implicit class ChannelFutureSyntaxExtension(val f: ChannelFuture) {
    def onComplete(fn: => Unit): ChannelFuture = {
      f.addListener(new GenericFutureListener[util.concurrent.Future[_ >: Void]] {
        override def operationComplete(future: util.concurrent.Future[_ >: Void]): Unit = fn
      })
      f
    }
  }

  private class InboundHandlerImpl(
    handshaker: WebSocketClientHandshaker,
    private val handler: WebsocketHandler
  ) extends SimpleChannelInboundHandler[ByteBufHolder] {

    private val handshakeFuture = Ref.make[ChannelPromise]()

    @volatile private var websocket: Websocket = _

    private val messageHandler: PartialFunction[MessageType, Unit] = { case frame =>
      if (handler.onMessage.isDefinedAt(frame)) handler.onMessage(frame)
      else handler.onUnhandledMessage(frame)
    }

    /** Blocks until handshake is completed.
      * @return
      */
    private[websocket] def blockUntilHandshaken(): Websocket = {
      atomic { implicit txn => handshakeFuture().sync() }
      websocket
    }

    override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
      atomic { implicit txn => handshakeFuture() = ctx.newPromise() }
    }

    override def channelActive(ctx: ChannelHandlerContext): Unit = {
      handshaker.handshake(ctx.channel())
      ()
    }

    override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBufHolder): Unit = {
      val ch = ctx.channel()
      val handleControlMessages: PartialFunction[ByteBufHolder, Unit] = {
        // handle Close initiated by Server
        case _: CloseWebSocketFrame =>
          try { handler.onClose(()) }
          catch { case NonFatal(ex) => handler.reportFailure(ex) }
          ch.close()
          ()

        // handshake completion
        case msg: FullHttpResponse =>
          if (!handshaker.isHandshakeComplete) {
            try {
              handshaker.finishHandshake(ch, msg)
              websocket = new WebsocketImpl(ch)
              handler._sender = websocket
              atomic { implicit txn => handshakeFuture().setSuccess() }
            } catch {
              case ex: WebSocketHandshakeException =>
                atomic { implicit txn => handshakeFuture().setFailure(ex) }
            }
            ()
          } else {
            throw new IllegalStateException(
              s"Unexpected FullHttpResponse (status=${msg.status}, content=${msg.content().toString(CharsetUtil.UTF_8)})"
            )
          }
      }

      val handleUserMessages: PartialFunction[ByteBufHolder, Unit] = {
        case f: MessageType if messageHandler.isDefinedAt(f) => messageHandler(f)
      }

      val handleKnownMessage = handleControlMessages orElse handleUserMessages
      try {
        if (handleKnownMessage.isDefinedAt(msg)) handleKnownMessage(msg)
        else {
          // unhandled
          println("UNHANDLED: " + msg)
        }
      } catch {
        case ex: Throwable => ex.printStackTrace()
      }
    }

    @Override
    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      ctx.close()
      atomic { implicit txn =>
        val f = handshakeFuture()
        if (!f.isDone) f.setFailure(cause)
        else handler.reportFailure(cause)
      }

      ()
    }
  }

  private class NettyClient(
    serverAddress: ServerAddress,
    options: Options
  ) extends WebsocketClient {
    import options._

    override lazy val implicits = new NettyImplicits with Implicits

    private lazy val loopGroup = new NioEventLoopGroup()

    private lazy val headers =
      options.headers.foldLeft(new DefaultHttpHeaders().asInstanceOf[HttpHeaders]) { case (headers, (k, v)) =>
        headers.add(k, v)
      }

    private def newWebsocket(handler: WebsocketHandler) = {
      val inboundHandler = {
        new InboundHandlerImpl(
          handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            serverAddress.build(),
            WebSocketVersion.V13,
            subProtocol.orNull,
            false,
            headers,
            maxFramePayloadLength
          ),
          handler = handler
        )
      }

      def initializer = new ChannelInitializer[SocketChannel]() {

        override def initChannel(ch: SocketChannel): Unit = {
          val p = ch.pipeline()

          for { sslCtx <- sslCtx } p.addLast(
            sslCtx.newHandler(ch.alloc(), serverAddress.host, serverAddress.port)
          )

          logLevel foreach { logLevel => p.addLast(new LoggingHandler(logLevel)) }

          p.addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(Int.MaxValue),
            new websocketx.WebSocketFrameAggregator(Int.MaxValue),
            inboundHandler
          )

          ()
        }
      }

      val boot = new Bootstrap()
        .group(loopGroup)
        .channel(classOf[NioSocketChannel])
        .handler(initializer)

      boot.connect(serverAddress.host, serverAddress.port).sync()

      inboundHandler.blockUntilHandshaken()
    }

    private def shutdown() =
      loopGroup.shutdownGracefully(shutdownQuietPeriod.toMillis, shutdownTimeout.toMillis, TimeUnit.MILLISECONDS)

    override def open(handler: WebsocketHandler): Websocket = {
      newWebsocket(handler)
    }

    def shutdownSync(): Unit = {
      shutdown().syncUninterruptibly()
      ()
    }

    def shutdownAsync(implicit ex: ExecutionContext): Future[Unit] = {
      val f = AdaptNettyFuture(shutdown())
      f map { _ => () }
    }
  }

  override def newClient(
    address: ServerAddress,
    options: Options = defaultOptions
  ): WebsocketClient = new NettyClient(address, options)

  class WebsocketImpl(ch: Channel) extends Websocket {

    private trait Tracer {
      def log(msg: String): Unit
      def log(msg: String, frame: MessageType): Unit
    }
    private object Trace {
      private val rnd    = new Random
      private val logger = LoggerFactory.getLogger("websocket-impl")
      def next(): Tracer = new Tracer {
        private val id = rnd.alphanumeric.take(4).mkString.toLowerCase
        def log(msg: String): Unit = {
          try {
            MDC.put("traceId", id)
            logger.debug(msg)
          } finally {
            MDC.clear()
          }
        }
        def log(msg: String, frame: MessageType): Unit = {
          try {
            val frameStr = frame.content().toString(StandardCharsets.UTF_8)
            MDC.put("traceId", id)
            logger.debug(msg + ": " + frameStr)
          } finally {
            MDC.clear()
          }
        }
      }
    }

    override protected def send(x: MessageType): Unit = {
      val trace = Trace.next()
      trace.log("sending", x)
      ch.writeAndFlush(x).onComplete { trace.log("sent", x) }
      ()
    }
    override def ping(): Unit = {
      val trace = Trace.next()
      trace.log("pinging")
      ch.writeAndFlush(new PingWebSocketFrame()).onComplete { trace.log("pinged") }
      ()
    }
    override def close()(implicit ec: ExecutionContext): Future[Unit] = {
      val trace = Trace.next()
      trace.log("closing")
      ch.writeAndFlush(new CloseWebSocketFrame()).onComplete { trace.log("closed") }
      val f = AdaptNettyFuture(ch.closeFuture())
      f.map { _ => () }
    }
  }
}
