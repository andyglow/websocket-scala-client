package com.github.andyglow.websocket

import com.github.andyglow.websocket.util.NettyFuture
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** Basic client-side websocket interface.
  */
trait Websocket {

  /** Send a message represented by one of a Scala types that is supported by [[MessageAdapter]].
    *
    * @param msg
    *   Message
    * @tparam T
    *   Message Type
    */
  def ![T: MessageAdapter](msg: T): Unit

  /** Alias for [[!]].
    *
    * @param msg
    *   Message
    * @tparam T
    *   Message Type
    */
  final def send[T: MessageAdapter](msg: T): Unit = this ! msg

  /** Send [[https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.1 Standard]] Close message and closes the socket.
    *
    * @param ec
    *   execution context
    * @return
    *   Scala's feature that can be used to control the websocket closing process.
    */
  def close()(implicit ec: ExecutionContext): Future[Unit]

  /** Send [[https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.2 Standard]] Ping message
    *
    * @param ec
    *   execution context
    */
  def ping()(implicit ec: ExecutionContext): Unit
}

/** Implementation of a client-side WebSocket based on Netty's Channel
  *
  * @param ch
  *   Netty's Channel
  */
private[websocket] class WebsocketImpl(ch: Channel) extends Websocket {

  override def ![T: MessageAdapter](msg: T): Unit =
    ch writeAndFlush { MessageAdapter[T] format msg }

  override def close()(implicit ec: ExecutionContext): Future[Unit] = {
    ch writeAndFlush new CloseWebSocketFrame
    val f = NettyFuture(ch.closeFuture())
    f map { _ => () }
  }

  override def ping()(implicit ec: ExecutionContext): Unit =
    ch.writeAndFlush(new PingWebSocketFrame())
}
