package com.github.andyglow.websocket.util

import io.netty.util.concurrent.{Future => NFuture}
import io.netty.util.concurrent.GenericFutureListener
import scala.concurrent.Future
import scala.concurrent.Promise

/** Translates Netty's Futures into Scala ones.
  */
object NettyFuture {

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
