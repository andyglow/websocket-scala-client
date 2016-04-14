package scalax.websocket

import io.netty.buffer.ByteBufHolder
import io.netty.channel.{ChannelHandlerContext, ChannelPromise, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx._
import io.netty.util.CharsetUtil

import scala.concurrent.stm._

private[websocket] class WebSocketClientHandler(
  handshaker: WebSocketClientHandshaker,
  listener: WebSocketMessageListener) extends SimpleChannelInboundHandler[ByteBufHolder] {

  private val handshakerFuture = Ref.make[ChannelPromise]()
  private[websocket] def waitForHandshake() = atomic { implicit txn => handshakerFuture().sync() }

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = atomic { implicit txn => handshakerFuture() = ctx.newPromise() }
  override def channelActive(ctx: ChannelHandlerContext): Unit = handshaker.handshake(ctx.channel())

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBufHolder): Unit = {
    val ch = ctx.channel()
    msg match {
      case msg: CloseWebSocketFrame => ch.close()
      case msg: TextWebSocketFrame => listener.onText(msg.text())
      case msg: BinaryWebSocketFrame => listener.onBinary(msg.content())
      case msg: FullHttpResponse =>
        if (!handshaker.isHandshakeComplete) {
          try {
            handshaker.finishHandshake(ch, msg)
            atomic { implicit txn => handshakerFuture().setSuccess() }
          } catch {
            case ex: WebSocketHandshakeException =>
              atomic { implicit txn => handshakerFuture().setFailure(ex) }
          }
        } else {
          val content = msg.content().toString(CharsetUtil.UTF_8)
          val status = msg.getStatus
          val errMsg = s"Unexpected FullHttpResponse (status=$status, content=$content)"
          throw new IllegalStateException(errMsg)
        }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    ctx.close()
    atomic { implicit txn =>
      val f = handshakerFuture()
      if (!f.isDone) f.setFailure(cause)
    }
  }

}
