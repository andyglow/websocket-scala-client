package com.github.andyglow.websocket.testserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer

import scala.concurrent.Future

trait CrossScalaVersionAkkaApi {

  def launchServer(
    interface: String,
    port: Int,
    handler: HttpRequest => HttpResponse)(implicit system: ActorSystem, mat: Materializer): Future[Http.ServerBinding] = {

    Http().newServerAt(interface, port).bindSync(handler)
  }
}
