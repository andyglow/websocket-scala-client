package com.github.andyglow.websocket

import akka.http.scaladsl.model.ws
import akka.stream.Materializer
import javax.net.ssl.SSLContext
import scala.concurrent.duration._

class AkkaPlatform extends Platform with AkkaClient {
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
    readStreamedMessageTimeout: FiniteDuration = 1.second,
    resolveTimeout: FiniteDuration = 5.second,
    sslCtx: Option[SSLContext] = None
  ) extends CommonOptions

  override type Options = AkkaOptions

  override def defaultOptions: AkkaOptions = AkkaOptions()

  override def newClient(address: ServerAddress, options: Options = defaultOptions): WebsocketClient =
    new PekkoClient(address, options)
}

object AkkaPlatform extends AkkaPlatform
