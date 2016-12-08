package com.github.andyglow.websocket

import io.netty.buffer.{ByteBuf, ByteBufHolder}
import io.netty.channel.{ChannelFuture, ChannelHandlerContext, ChannelPromise, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx._
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory

import scala.concurrent.stm._

private[websocket] class WebsocketNettytHandler[T : MessageFormat](
  handshaker: WebsocketNettyHandshaker,
  private val handler: WebsocketHandler[T]) extends SimpleChannelInboundHandler[ByteBufHolder] {

  private val logger = LoggerFactory.getLogger(classOf[WebsocketNettytHandler[T]])

  private val handshakerFuture = Ref.make[ChannelPromise]()
  @volatile private var websocket: Websocket = _

  private val msgHandler: PartialFunction[ByteBufHolder, Unit] = {
    implicitly[MessageFormat[T]] match {
      case x if x == MessageFormat.String => {
        case msg: TextWebSocketFrame =>
          try {
            handler.asInstanceOf[WebsocketHandler[String]].receive(msg.text())
          } catch {
            case ex: Throwable => logger.error("Error matching frame", ex)
          }
      }
      case x if x == MessageFormat.ByteBuf => {
        case msg: BinaryWebSocketFrame =>
          try {
            handler.asInstanceOf[WebsocketHandler[ByteBuf]].receive(msg.content())
          } catch {
            case ex: Throwable => logger.error("Error matching frame", ex)
          }
      }
    }
  }

  private[websocket] def waitForHandshake(): Websocket = {
    atomic { implicit txn => handshakerFuture().sync() }
    websocket
  }

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    atomic { implicit txn => handshakerFuture() = ctx.newPromise() }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    handshaker.handshake(ctx.channel())
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBufHolder): Unit = {
    val ch = ctx.channel()
    def controls: PartialFunction[ByteBufHolder, ChannelFuture] = {
        case msg: CloseWebSocketFrame   => ch.close()
        case msg: FullHttpResponse      =>
          if (!handshaker.isHandshakeComplete) {
            try {
              handshaker.finishHandshake(ch, msg)
              websocket = new WebsocketImpl(ch)
              handler._sender = websocket
              atomic { implicit txn => handshakerFuture().setSuccess() }
            } catch {
              case ex: WebSocketHandshakeException =>
                atomic { implicit txn => handshakerFuture().setFailure(ex) }
            }
          } else {
            val content = msg.content().toString(CharsetUtil.UTF_8)
            val status = msg.status
            val errMsg = s"Unexpected FullHttpResponse (status=$status, content=$content)"
            throw new IllegalStateException(errMsg)
          }
      }

    (controls orElse msgHandler)(msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    ctx.close()
    atomic { implicit txn =>
      val f = handshakerFuture()
      if (!f.isDone) f.setFailure(cause)
    }
  }

}
