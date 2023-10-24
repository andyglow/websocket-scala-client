package com.github.andyglow.websocket

import java.util.concurrent.{Future => JFut}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

private[websocket] object AdaptJdkFuture {

  def apply[T](f: JFut[T])(implicit ec: ExecutionContext): Future[T] = {
    if (f.isDone) {
      Future(f.get())
    } else {
      val promise = Promise[T]()
      ec.execute { () =>
        try promise.success(f.get)
        catch {
          case x: Throwable => promise.failure(x)
        }
      }
      promise.future
    }
  }
}
