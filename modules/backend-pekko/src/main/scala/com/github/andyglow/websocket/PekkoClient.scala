package com.github.andyglow.websocket

import org.apache.pekko.Done
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.ConnectionContext
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.ws
import org.apache.pekko.http.scaladsl.model.ws.Message
import org.apache.pekko.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait PekkoClient extends PekkoImplicits { this: PekkoPlatform =>

  class PekkoClient(address: ServerAddress, options: Options) extends WebsocketClient {
    implicit val system: ActorSystem = ActorSystem()

    implicit lazy val ic: InternalContext = PekkoInternalContext(options, Materializer(system))

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
  }
}
