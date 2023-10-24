package com.github.andyglow.websocket.testserver

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream._
import akka.stream.scaladsl.Flow


object TestServer extends PlatformDependent {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: Materializer = SystemMaterializer(system).materializer

    val service = Flow[Message]
      .via(new ConnectionTerminatingGraphStage({
        case x: TextMessage if x.getStrictText == "close-connection" =>
      }))
      .mapConcat {
        case tm: TextMessage   => TextMessage(tm.textStream) :: Nil
        case bm: BinaryMessage => BinaryMessage(bm.dataStream) :: Nil
      }

    val requestHandler: HttpRequest => HttpResponse = {
      (req: HttpRequest) =>
        websocketAttribution(req) match {
          case Some(upgrade) => upgrade handleMessages service
          case None          => HttpResponse(400, entity = "Not a valid websocket request!")
        }
    }

    val bindingFuture =
      launchServer(interface = "localhost", port = 8080, requestHandler)

    println(s"Test WS Server is listening at http://localhost:8080/\nPress RETURN to stop...")
    scala.io.StdIn.readLine()

    import system.dispatcher // for the future transformations
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

