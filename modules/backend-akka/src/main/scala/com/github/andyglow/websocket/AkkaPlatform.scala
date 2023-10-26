package com.github.andyglow.websocket

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.model.ws.Message
import akka.stream.Materializer

import javax.net.ssl.SSLContext
import scala.concurrent.duration._

class AkkaPlatform extends Platform with AkkaClient {

  // NOTE: this article describes alternative way to handle incoming messages so that when you parse them they always Strict versions
  //       https://dev-listener.medium.com/learning-to-tame-akka-http-websocket-c4cec2cdf23f
  //       see how the `incoming` Sink gets created
  //       Tis may allow some simplification at Message Adapter side

  override type MessageType     = ws.Message
  override type Binary          = ws.BinaryMessage
  override type Text            = ws.TextMessage
  override type Pong            = ws.Message // akka-http doesn't support api level ping/pong
  override type InternalContext = AkkaInternalContext

  case class AkkaInternalContext(
    options: AkkaOptions,
    mat: Materializer
  )

  case class AkkaOptions(
    override val headers: Map[String, String] = Map.empty,
    override val subProtocol: Option[String] = None,
    override val tracer: Tracer = acceptingAnyPF(()),
    readStreamedMessageTimeout: FiniteDuration = 1.second,
    resolveTimeout: FiniteDuration = 5.second,
    sslCtx: Option[SSLContext] = None,
    getSystem: () => ActorSystem = () => ActorSystem(),
    handlerParallelism: Int = java.lang.Runtime.getRuntime.availableProcessors()
  ) extends CommonOptions {
    override def withTracer(tracer: Tracer): AkkaOptions = copy(tracer = tracer)
  }

  override type Options = AkkaOptions

  override def defaultOptions: AkkaOptions = AkkaOptions()

  override def newClient(address: ServerAddress, options: Options = defaultOptions): WebsocketClient =
    new AkkaClient(address, options)

  override val implicits: MessageAdapter.Implicits with Implicits = new AkkaImplicits

  override protected def stringify(x: Message): String = x match {
    case ws.TextMessage.Strict(x) => s"text[$x]"
    case ws.TextMessage.Streamed(_) => "text(...)"
    case ws.BinaryMessage.Strict(x) => s"binary[${x.encodeBase64.mkString}]"
    case ws.BinaryMessage.Streamed(_) => "binary(...)"
  }
}

object AkkaPlatform extends AkkaPlatform
