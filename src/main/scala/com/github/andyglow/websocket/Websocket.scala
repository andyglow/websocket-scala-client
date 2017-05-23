package com.github.andyglow.websocket

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx._

import scala.concurrent.{ExecutionContext, Future}

trait Websocket {
  def ![T : MessageFormat](msg: T): Unit
  def close(implicit ec: ExecutionContext): Future[Unit]
}

private[websocket] class WebsocketImpl(ch: Channel) extends Websocket {

  override def ![T : MessageFormat](msg: T): Unit = implicitly[MessageFormat[T]] match {
    case MessageFormat.String  => ch writeAndFlush new TextWebSocketFrame(msg.asInstanceOf[String])
    case MessageFormat.ByteBuf => ch writeAndFlush new BinaryWebSocketFrame(msg.asInstanceOf[ByteBuf])
  }

  override def close(implicit ec: ExecutionContext): Future[Unit] = {
    ch writeAndFlush new CloseWebSocketFrame()
    val f = NettyFuture(ch.closeFuture())
    f map {_ => ()}
  }
}
