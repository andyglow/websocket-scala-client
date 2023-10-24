package com.github.andyglow.websocket

import org.apache.pekko.http.scaladsl.model._

import javax.net.ssl.SSLContext
import scala.concurrent.duration._

class PekkoPlatform extends Platform with PekkoClient {
  override type MessageType  = ws.Message
  override type Binary = ws.BinaryMessage
  override type Text   = ws.TextMessage
  override type Pong   = ws.Message // akka-http doesn't support api level ping/pong

  case class PekkoOptions(
    override val headers: Map[String, String] = Map.empty,
    override val subProtocol: Option[String] = None,
    readStreamedMessageTimeout: FiniteDuration = 1.second,
    resolveTimeout: FiniteDuration = 5.second,
    sslCtx: Option[SSLContext] = None
  ) extends CommonOptions

  override type Options = PekkoOptions

  override def defaultOptions: PekkoOptions = PekkoOptions()

  override def newClient(address: ServerAddress, options: Options = defaultOptions): WebsocketClient = new PekkoClient(address, options)
}

object PekkoPlatform extends PekkoPlatform