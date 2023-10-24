package com.github.andyglow.websocket

import akka.Done
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.model.ws.Message
import akka.stream.CompletionStrategy
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait AkkaClient extends AkkaImplicits { this: AkkaPlatform =>

  class PekkoClient(address: ServerAddress, options: Options) extends WebsocketClient {
    implicit val system: ActorSystem = ActorSystem()

    override def open(handler: WebsocketHandler): Websocket = new Websocket {

      private val flow = options.sslCtx match {
        case Some(sslCtx) =>
          Http().webSocketClientFlow(
            request = ws.WebSocketRequest(uri = address.build().toString),
            connectionContext = ConnectionContext.httpsClient(sslCtx)
          )
        case None =>
          Http().webSocketClientFlow(
            ws.WebSocketRequest(uri = address.build().toString)
          )
      }

      private val outgoing: Source[ws.Message, ActorRef] =
        Source.actorRef(
          completionMatcher = { case Done =>
            CompletionStrategy.immediately
          },
          failureMatcher = PartialFunction.empty,
          bufferSize = 10,
          OverflowStrategy.fail
        )

      private val incoming: Sink[ws.Message, Future[Done]] = {
        Sink.foreach(handler.onMessage)
      }

      private val ((actor, upgradeResponse), closed) =
        outgoing
          .viaMat(flow)(Keep.both)
          .toMat(incoming)(Keep.both)
          .run()

      private val connected = upgradeResponse.flatMap { upgrade =>
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
          Future.successful(Done)
        } else {
          throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
        }
      }(system.dispatcher)

      handler._sender = this

      override protected def send(x: Message): Unit = actor ! x

      override def ping(): Unit = ()

      override def close()(implicit ec: ExecutionContext): Future[Unit] = {
        actor ! Done
        connected.map(_ => ())
      }
    }

    override def shutdownSync(): Unit = {
      system.terminate().map(_ => ())(system.dispatcher)
    }

    override def shutdownAsync(implicit ec: ExecutionContext): Future[Unit] = {
      system.terminate() map { _ => () }
    }

    override lazy val implicits: MessageAdapter.Implicits with Implicits = new PekkoImplicits(options)
  }
}
