package com.github.andyglow.websocket

import org.apache.pekko.Done
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.ConnectionContext
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.ws
import org.apache.pekko.http.scaladsl.model.ws.Message
import org.apache.pekko.stream.CompletionStrategy
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.Timeout
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait PekkoClient extends PekkoImplicits { this: PekkoPlatform =>

  class PekkoClient(address: ServerAddress, options: Options) extends WebsocketClient {
    implicit val system: ActorSystem = ActorSystem()
    implicit val mat: Materializer   = Materializer.matFromSystem(system)
    implicit val ic: InternalContext = PekkoInternalContext(options, mat)
    private val tracer               = options.tracer.lift.andThen(_ => ())

    override def open(handler: WebsocketHandler): Websocket =
      new PekkoWebsocket(handler, options.tracer.lift.andThen(_ => ())) with Websocket.TracingAsync

    class PekkoWebsocket(handler: WebsocketHandler, val trace: Trace) extends Websocket with Websocket.AsyncImpl {
      override protected implicit val executionContext: ExecutionContext = system.dispatcher
      override protected def timeout: FiniteDuration                     = options.resolveTimeout

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
        Sink.foreachAsync(options.handlerParallelism) { msg =>
          tracer(Websocket.TracingEvent.Received(msg))
          Future { handler.onMessage(msg) }
        }
      }

      private val ((actor, upgradeResponse), _) =
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

      implicit val askTimeout: Timeout = options.resolveTimeout

      override protected def sendAsync(x: Message): Future[Unit] = Future { actor ! x }

      override protected def pingAsync(): Future[Unit] = Future.successful(())

      override protected def closeAsync(): Future[Unit] = for {
        _ <- Future { actor ! Done }
        _ <- connected
      } yield ()
    }

    override def shutdown(): Unit = {
      Await.result(system.terminate().map(_ => ())(system.dispatcher), options.resolveTimeout)
    }
  }
}
