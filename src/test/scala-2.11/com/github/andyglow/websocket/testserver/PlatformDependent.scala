package com.github.andyglow.websocket.testserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer

import java.util.concurrent.Callable
import scala.concurrent.Future

trait PlatformDependent {

  @inline def launchServer(
    interface: String,
    port: Int,
    handler: HttpRequest => HttpResponse)(implicit system: ActorSystem, mat: Materializer): Future[Http.ServerBinding] = {

    Http().bindAndHandleSync(handler, interface, port)
  }

  @inline def websocketAttribution(x: HttpRequest): Option[UpgradeToWebSocket] = x.header[UpgradeToWebSocket]
}

object PlatformDependent {

  @inline def callable[T](fn: () => T): Callable[T] = new Callable[T] {
    def call(): T = fn()
  }
}
