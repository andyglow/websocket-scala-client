package com.github.andyglow.websocket

import javax.net.ssl.SSLContext
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.ws.Message
import org.apache.pekko.stream.Materializer

import scala.concurrent.duration._

class PekkoPlatform extends Platform with PekkoClient {
  override type MessageType     = ws.Message
  override type Binary          = ws.BinaryMessage
  override type Text            = ws.TextMessage
  override type Pong            = ws.Message // akka-http doesn't support api level ping/pong
  override type InternalContext = PekkoInternalContext

  case class PekkoInternalContext(
    options: PekkoOptions,
    mat: Materializer
  )

  case class PekkoOptions(
    override val headers: Map[String, String] = Map.empty,
    override val subProtocol: Option[String] = None,
    override val tracer: Tracer = acceptingAnyPF(()),
    readStreamedMessageTimeout: FiniteDuration = 1.second,
    resolveTimeout: FiniteDuration = 5.second,
    sslCtx: Option[SSLContext] = None,
    getSystem: () => ActorSystem = () => ActorSystem(),
    handlerParallelism: Int = java.lang.Runtime.getRuntime.availableProcessors()
  ) extends CommonOptions {
    override def withTracer(tracer: Tracer): PekkoOptions = PekkoOptions(tracer = tracer)
  }

  override type Options = PekkoOptions

  override def defaultOptions: PekkoOptions = PekkoOptions()

  override def newClient(address: ServerAddress, options: Options = defaultOptions): WebsocketClient =
    new PekkoClient(address, options)

  override val implicits: MessageAdapter.Implicits with Implicits = new PekkoImplicits

  override protected def stringify(x: Message): String = x match {
    case ws.TextMessage.Strict(x) => s"text[$x]"
    case ws.TextMessage.Streamed(_) => "text(...)"
    case ws.BinaryMessage.Strict(x) => s"binary[${x.encodeBase64.mkString}]"
    case ws.BinaryMessage.Streamed(_) => "binary(...)"
  }
}

object PekkoPlatform extends PekkoPlatform
