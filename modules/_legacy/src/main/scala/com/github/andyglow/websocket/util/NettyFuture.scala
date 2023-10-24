package com.github.andyglow.websocket.util

import io.netty.util.concurrent.{Future => NFuture}
import io.netty.util.concurrent.GenericFutureListener
import scala.concurrent.Future
import scala.concurrent.Promise

/** Translates Netty's Futures into Scala ones.
  */
object NettyFuture {

  def apply[T](f: NFuture[T]): Future[T] = {
    if (f.isDone) {
      if (f.isSuccess) Future.successful(f.getNow) else Future.failed(f.cause())
    } else {
      val promise = Promise[T]()
      val futureListener = new GenericFutureListener[NFuture[T]]() {
        override def operationComplete(future: NFuture[T]): Unit = {
          if (future.isSuccess) promise.success(future.getNow) else promise.failure(future.cause())
          ()
        }
      }
      f.addListener(futureListener)
      promise.future
    }
  }
}
