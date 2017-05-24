package com.github.andyglow.websocket.testserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.stage._

object TestServer {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val service = Flow[Message]
      .via(new ConnectionTerminatingGraphStage({
        case x: TextMessage if x.getStrictText == "close-connection" =>
      }))
      .mapConcat {
        case tm: TextMessage => TextMessage(tm.textStream) :: Nil
        case bm: BinaryMessage =>
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }

    val requestHandler: HttpRequest => HttpResponse = {
      req: HttpRequest =>
        req.header[UpgradeToWebSocket] match {
          case Some(upgrade) => upgrade handleMessages service
          case None          => HttpResponse(400, entity = "Not a valid websocket request!")
        }
    }

    val bindingFuture =
      Http().bindAndHandleSync(requestHandler, interface = "localhost", port = 8080)

    println(s"Test WS Server is listening at http://localhost:8080/\nPress RETURN to stop...")
    scala.io.StdIn.readLine()

    import system.dispatcher // for the future transformations
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

