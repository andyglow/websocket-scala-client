package com.github.andyglow.websocket

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx._

trait Websocket {
  def ![T : MessageFormat](msg: T): Unit
  def close(): Unit
}

private[websocket] class WebsocketImpl(ch: Channel) extends Websocket {

  override def ![T : MessageFormat](msg: T): Unit = implicitly[MessageFormat[T]] match {
    case StringFormat  => ch writeAndFlush new TextWebSocketFrame(msg.asInstanceOf[String])
    case ByteBufFormat => ch writeAndFlush new BinaryWebSocketFrame(msg.asInstanceOf[ByteBuf])
  }

  override def close(): Unit = {
    ch.writeAndFlush(new CloseWebSocketFrame())
    ch.closeFuture()
  }

}
