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
import io.netty.util.CharsetUtil
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.stm._
import scala.util.control.NonFatal

// TODO: re-work without using STM. We should be able to get it done relying on
//       Netty's Feature/Promise stuff
trait NettyClient { this: NettyPlatform =>

  private class InboundHandlerImpl(
    handshaker: WebSocketClientHandshaker,
    options: Options,
    private val handler: WebsocketHandler,
    executor: Executor
  ) extends SimpleChannelInboundHandler[ByteBufHolder] {

    // needed only to run message handling in a separate thread,
    // otherwise closing a websocket from within a message handler block blocking the closing future resolution
    // as Await.result blocks it
    private def schedule(fn: => Unit): Unit = {
      executor.execute(new Runnable {
        override def run(): Unit = fn
      })
    }

    private val handshakeFuture = Ref.make[ChannelPromise]()

    @volatile private var websocket: Websocket = _

    private val messageHandler: MessageType => Unit = { frame =>
      options.tracer(Websocket.TracingEvent.Received(frame))
      schedule {
        if (handler.onMessage.isDefinedAt(frame)) handler.onMessage(frame)
        else handler.onUnhandledMessage(frame)
      }
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
      val ch                                                          = ctx.channel()
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
              websocket = new NettyWebsocket(
                ch,
                options.futureResolutionTimeout,
                options.tracer.lift.andThen(_ => ()),
                options.executionContext
              ) with Websocket.TracingAsync
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

      val handleUserMessages: PartialFunction[ByteBufHolder, Unit] = { case f: MessageType =>
        f.content().retain()
        messageHandler(f)
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

    override implicit val ic: InternalContext = NettyInternalContext

    private lazy val loopGroup = new NioEventLoopGroup()

    private lazy val headers =
      options.headers.foldLeft(new DefaultHttpHeaders().asInstanceOf[HttpHeaders]) { case (headers, (k, v)) =>
        headers.add(k, v)
      }

    private def newWebsocket(handler: WebsocketHandler) = {
      val inboundHandler = new InboundHandlerImpl(
        handshaker = WebSocketClientHandshakerFactory.newHandshaker(
          serverAddress.build(),
          WebSocketVersion.V13,
          subProtocol.orNull,
          false,
          headers,
          maxFramePayloadLength
        ),
        handler = handler,
        options = options,
        executor = loopGroup
      )

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

    override def open(handler: WebsocketHandler): Websocket = {
      newWebsocket(handler)
    }

    def shutdown(): Unit = {
      loopGroup
        .shutdownGracefully(shutdownQuietPeriod.toMillis, shutdownTimeout.toMillis, TimeUnit.MILLISECONDS)
        .syncUninterruptibly()
      ()
    }
  }

  override def newClient(
    address: ServerAddress,
    options: Options = defaultOptions
  ): WebsocketClient = new NettyClient(address, options)

  class NettyWebsocket(
    ch: Channel,
    val timeout: FiniteDuration,
    val trace: Trace,
    ec: ExecutionContext
  ) extends Websocket
      with Websocket.AsyncImpl {
    override protected implicit val executionContext: ExecutionContext = ec
    override protected def sendAsync(x: MessageType): Future[Unit]     = {
      AdaptNettyFuture(ch.writeAndFlush(x)).map { _ => () }
    }
    override protected def pingAsync(): Future[Unit] = {
      AdaptNettyFuture(ch.writeAndFlush(new PingWebSocketFrame())).map(_ => ())
    }
    override protected def closeAsync(): Future[Unit] = {
      ch.writeAndFlush(new CloseWebSocketFrame())
      AdaptNettyFuture { ch.closeFuture() }.map(_ => ())
    }
  }
}
