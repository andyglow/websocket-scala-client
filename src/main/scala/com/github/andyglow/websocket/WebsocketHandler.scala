package com.github.andyglow.websocket

import scala.concurrent.{ExecutionContext, Future}

trait WebsocketHandler[T] {
  @volatile private[websocket] var _sender: Websocket = WebsocketHandler.NoSocket
  def sender(): Websocket = _sender
  def receive: PartialFunction[T, Unit]
  def onFailure: PartialFunction[Throwable, Unit] = {
    case x: Throwable => x.printStackTrace() // ignore errors
  }
  def onClose: Unit => Unit = identity
  private[websocket] def reportFailure(th: Throwable): Unit = if (onFailure isDefinedAt th) onFailure(th)
}

object WebsocketHandler {
  val NoSocket: Websocket = new Websocket {
    override def ![T: MessageFormat](msg: T): Unit = ()
    override def close(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())
  }

  def apply[T : MessageFormat](
    pf: PartialFunction[T, Unit],
    exceptionHandler: PartialFunction[Throwable, Unit] = PartialFunction.empty,
    closeHandler: Unit => Unit = identity) = new WebsocketHandler[T] {

    override val receive: PartialFunction[T, Unit] = pf
    override val onFailure: PartialFunction[Throwable, Unit] = exceptionHandler
    override val onClose: Unit => Unit = closeHandler
  }
}