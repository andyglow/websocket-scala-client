package com.github.andyglow.websocket

import akka.Done
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.WebSocketUpgradeResponse
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait AkkaClient extends AkkaImplicits { this: AkkaPlatform =>

  class AkkaClient(address: ServerAddress, options: Options) extends WebsocketClient {
    val system: ActorSystem          = options.getSystem()
    implicit val mat: Materializer   = Materializer.matFromSystem(system)
    implicit val ic: InternalContext = AkkaInternalContext(options, mat)
    private val tracer               = options.tracer.lift.andThen(_ => ())

    override def open(handler: WebsocketHandler): Websocket =
      new AkkaWebsocket(handler, options.tracer.lift.andThen(_ => ())) with Websocket.TracingAsync

    class AkkaWebsocket(handler: WebsocketHandler, val trace: Trace) extends Websocket with Websocket.AsyncImpl {
      override protected implicit val executionContext: ExecutionContext = system.dispatcher
      override protected def timeout: FiniteDuration                     = options.resolveTimeout

      private val flow: Flow[Message, Message, Future[WebSocketUpgradeResponse]] = Http()(system).webSocketClientFlow(
        request = ws.WebSocketRequest(uri = address.build().toString),
        connectionContext = options.sslCtx match {
          case Some(sslCtx) => ScalaRuntime.akka_http_ConnectionContext_httpsClient(sslCtx)
          case None         => ConnectionContext.noEncryption()
        }
      )

      private val outgoing: Source[ws.Message, ActorRef] = ScalaRuntime.akka_stream_Source_actorRef()

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
      }

      handler._sender = this

      implicit val askTimeout: Timeout = options.resolveTimeout

      override protected def sendAsync(x: Message): Future[Unit] = Future { actor ! x }

      // explicit ping is not supported by Akka-Http
      override protected def pingAsync(): Future[Unit] = Future.successful(())

      override protected def closeAsync(): Future[Unit] = for {
        _ <- Future { actor ! ScalaRuntime.akka_stream_completion_message }
        _ <- connected
      } yield ()
    }

    override def shutdown(): Unit = {
      Await.result(system.terminate().map(_ => ())(system.dispatcher), options.resolveTimeout)
    }
  }
}
