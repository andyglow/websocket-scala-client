package com.github.andyglow.websocket.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufUtil
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel._
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.codec.http.websocketx.{BinaryWebSocketFrame, TextWebSocketFrame, WebSocketFrame, WebSocketServerProtocolHandler}
import io.netty.handler.codec.http._
import io.netty.handler.logging.LoggingHandler

import scala.util.Try

class WebsocketServer(
  val port: Int,
  private val ssl: Boolean,
  private val bossGroup: NioEventLoopGroup,
  private val workerGroup: NioEventLoopGroup,
  private val ch: Try[Channel]
) {

  def dumpStatus(): Unit = {
    println(s"WebsocketServer[${if (ch.isSuccess) "👍🏻" else "👎🏻"}]")
    ch foreach { ch =>
      println(s"- active  : ${ch.isActive}")
      println(s"- open    : ${ch.isOpen}")
      println(s"- port    : ${port}")
      println(s"- ssl     : ${ssl}")
      println(s"- address : ${if (ssl) "wss" else "ws"}://localhost:$port${WebsocketServer.WebsocketPath}")
    }
  }

  def runUntilChannelClosed(): Unit = {
    // close channel
    ch foreach { ch =>
      ch.closeFuture().sync()
      ()
    }

    shutdown()
  }

  def shutdown(): Unit = {
    workerGroup.shutdownGracefully().sync()
    bossGroup.shutdownGracefully().sync()
    ()
  }
}

object WebsocketServer {

  val WebsocketPath: String = "/websocket"

  private def freePort(): Int = {
    import java.net._
    val port = for {
      socket <- Try { new ServerSocket(0) }
      port   <- Try { socket.getLocalPort }
      _      <- Try { socket.close() }
    } yield port

    port.get
  }

  def newServer(ssl: Boolean): WebsocketServer = newServer(freePort(), ssl)

  def newServer(port: Int, ssl: Boolean): WebsocketServer = {
    val bossGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup()
    val ch = Try {
      val b = new ServerBootstrap()
      b.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler())
        .childHandler(new ChannelInitializer[SocketChannel] {
          override def initChannel(ch: SocketChannel): Unit = {
            val pipeline = ch.pipeline()
            if (ssl) pipeline.addLast(WsSsl.selfSignedContext.newHandler(ch.alloc))
            pipeline.addLast("http-serve", new HttpServerCodec)
            pipeline.addLast("http-agg", new HttpObjectAggregator(8192))
            pipeline.addLast("http-handle", new SimpleChannelInboundHandler[FullHttpRequest] {
              import io.netty.handler.codec.http.HttpResponseStatus._

              override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
                cause.printStackTrace()
                ctx.close()
                ()
              }

              override def channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
                def sendHttpResponse(responseStatus: HttpResponseStatus): Unit = {
                  // Generate an error page if response getStatus code is not OK (200).
                  val res = new DefaultFullHttpResponse(req.protocolVersion, responseStatus, ctx.alloc.buffer(0))
                  if (responseStatus.code != 200) {
                    ByteBufUtil.writeUtf8(res.content, responseStatus.toString)
                    HttpUtil.setContentLength(res, res.content.readableBytes.toLong)
                  }
                  // Send the response and close the connection if necessary.
                  val keepAlive = HttpUtil.isKeepAlive(req) && (responseStatus.code == 200)
                  HttpUtil.setKeepAlive(res, keepAlive)
                  val future = ctx.writeAndFlush(res)
                  if (!keepAlive) future.addListener(ChannelFutureListener.CLOSE)
                  ()
                }

                if (!req.decoderResult.isSuccess) {
                  sendHttpResponse(BAD_REQUEST)
                  return
                }

                // Handle websocket upgrade request.// Handle websocket upgrade request.
                if (req.headers.contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true)) {
                  ctx.fireChannelRead(req.retain())
                  return
                }

                sendHttpResponse(NOT_FOUND);
              }
            })
            pipeline.addLast("ws-compress", new WebSocketServerCompressionHandler)
            pipeline.addLast("ws-proto-handle", new WebSocketServerProtocolHandler(WebsocketPath, null, true, 65536, false, false, false))
            pipeline.addLast("ws-logger", new LoggingHandler)
            pipeline.addLast("ws-frame-handle", new SimpleChannelInboundHandler[WebSocketFrame] {
              override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame): Unit = {
                frame match {
                  case frame: TextWebSocketFrame =>
                    ctx.channel.writeAndFlush(new TextWebSocketFrame(frame.text()))
                  case frame: BinaryWebSocketFrame =>
                    frame.content().retain()
                    ctx.channel.writeAndFlush(new BinaryWebSocketFrame(frame.content()))
                  case _ =>
                }
                ()
              }
            })

            ()
          }
        })
      b.bind(port).sync.channel
    }
    new WebsocketServer(port, ssl, bossGroup, workerGroup, ch)
  }

  def main(args: Array[String]): Unit = {
    val ws = WebsocketServer.newServer(9098, ssl = false)
    ws.dumpStatus()
    ws.runUntilChannelClosed()
  }
}