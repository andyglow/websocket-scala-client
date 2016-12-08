package com.github.andyglow.websocket

import io.netty.util.concurrent.{GenericFutureListener, Future => NFuture}

import scala.concurrent.{Future, Promise}

private [websocket] object NettyFuture {

  def apply[T](f: NFuture[T]): Future[T] = {
    val p = Promise[T]()
    val l = new GenericFutureListener[NFuture[T]]() {
      override def operationComplete(future: NFuture[T]): Unit = {
        if (future.isSuccess) p.success(future.getNow) else p.failure(future.cause())
      }
    }
    f addListener l
    p.future
  }

}
