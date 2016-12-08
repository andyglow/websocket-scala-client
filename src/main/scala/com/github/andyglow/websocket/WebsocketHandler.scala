package com.github.andyglow.websocket

import scala.concurrent.{ExecutionContext, Future}

trait WebsocketHandler[T] {
  @volatile private[websocket] var _sender: Websocket = WebsocketHandler.NoSocket
  def sender(): Websocket = _sender
  def receive: PartialFunction[T, Unit]
}

object WebsocketHandler {
  val NoSocket: Websocket = new Websocket {
    override def ![T: MessageFormat](msg: T): Unit = ()
    override def close(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())
  }
  def apply[T : MessageFormat](pf: PartialFunction[T, Unit]) = new WebsocketHandler[T] {
    override def receive: PartialFunction[T, Unit] = pf
  }
}