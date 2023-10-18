package com.github.andyglow.websocket.testserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.AttributeKeys
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.WebSocketUpgrade
import akka.stream.Materializer
import java.util.concurrent.Callable
import scala.concurrent.Future

trait PlatformDependent {

  @inline def launchServer(interface: String, port: Int, handler: HttpRequest => HttpResponse)(implicit
    system: ActorSystem,
    mat: Materializer
  ): Future[Http.ServerBinding] = {

    Http().newServerAt(interface, port).bindSync(handler)
  }

  @inline def websocketAttribution(x: HttpRequest): Option[WebSocketUpgrade] =
    x.attribute(AttributeKeys.webSocketUpgrade)
}

object PlatformDependent {

  @inline def callable[T](fn: () => T): Callable[T] = () => fn()
}
