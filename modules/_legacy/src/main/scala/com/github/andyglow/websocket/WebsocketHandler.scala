package com.github.andyglow.websocket

import io.netty.buffer.ByteBufHolder
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** WebSocket Handler is a Server Message Handler
  * @tparam T
  *   Message Type
  */
trait WebsocketHandler[T] {

  @volatile private[websocket] var _sender: Websocket = WebsocketHandler.NoopSocket

  def sender(): Websocket = _sender

  def onMessage: PartialFunction[T, Unit]

  def onUnhandledMessage: Function[T, Unit] = _ => ()

  def onUnhandledNettyMessage: Function[ByteBufHolder, Unit] = _ => ()

  def onFailure: PartialFunction[Throwable, Unit] = { case _: Throwable => /* ignore errors */ }

  def onClose: Unit => Unit = identity

  private[websocket] def reportFailure(th: Throwable): Unit = if (onFailure isDefinedAt th) onFailure(th)
}

object WebsocketHandler {

  val NoopSocket: Websocket = new Websocket {

    override def ![T: MessageAdapter](msg: T): Unit = ()

    override def close()(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())

    override def ping()(implicit ec: ExecutionContext): Unit = ()

  }

  def apply[T: MessageAdapter](
    pf: PartialFunction[T, Unit],
    exceptionHandler: PartialFunction[Throwable, Unit] = PartialFunction.empty,
    unhandledMessageHandler: Function[T, Unit] = (_: T) => (),
    unhandledNettyMessageHandler: Function[ByteBufHolder, Unit] = _ => (),
    closeHandler: Unit => Unit = identity
  ): WebsocketHandler[T] = {

    new WebsocketHandler[T] {

      override val onUnhandledMessage: Function[T, Unit] = unhandledMessageHandler

      override val onUnhandledNettyMessage: Function[ByteBufHolder, Unit] = unhandledNettyMessageHandler

      override val onMessage: PartialFunction[T, Unit] = pf

      override val onFailure: PartialFunction[Throwable, Unit] = exceptionHandler

      override val onClose: Unit => Unit = closeHandler
    }
  }
}
