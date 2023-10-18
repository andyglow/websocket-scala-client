package com.github.andyglow.websocket

import io.netty.buffer.ByteBufHolder
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx._
import io.netty.util.CharsetUtil
import scala.concurrent.stm._
import scala.util.control.NonFatal

/** Implementation of Netty's ChannelInboundHandler
  * @param handshaker
  *   Netty's WS handshaker
  * @param handler
  *   Websocket Handler
  * @param adapter
  *   Message Adapter
  * @tparam T
  *   Message Type
  */
private[websocket] class WebsocketNettyHandler[T](
  handshaker: WebSocketClientHandshaker,
  private val handler: WebsocketHandler[T]
)(implicit adapter: MessageAdapter[T])
    extends SimpleChannelInboundHandler[ByteBufHolder] {

  private val handshakeFuture = Ref.make[ChannelPromise]()

  @volatile private var websocket: Websocket = _

  /* Create unified message handler that handles Netty's level of messages, translates them into adapted ones and
   * runs though a number of handlers.
   * - handler.onMessage gets called when message is adapted and recognized
   * - handler.onUnhandledMessage gets called when message is adapted, but not recognized
   * - handler.onUnhandledNettyMessage gets called when message is not adapted
   */
  private val messageHandler: PartialFunction[ByteBufHolder, Unit] = { case nettyMsg =>
    adapter.parse(nettyMsg).fold(handler.onUnhandledNettyMessage(nettyMsg)) { msg =>
      if (handler.onMessage.isDefinedAt(msg))
        handler.onMessage(msg)
      else
        handler.onUnhandledMessage(msg)
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
    val ch = ctx.channel()
    def controls: PartialFunction[ByteBufHolder, Unit] = {
      case _: CloseWebSocketFrame =>
        try { handler.onClose(()) }
        catch { case NonFatal(ex) => handler.reportFailure(ex) }
        ch.close()
        ()

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

    (controls orElse messageHandler)(msg)
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
