package com.github.andyglow.websocket

import java.nio.ByteBuffer
import javax.net.ssl.SSLContext

class JdkPlatform extends Platform with JdkImplicits with JdkClient {
  override type MessageType     = Any
  override type Binary          = ByteBuffer
  override type Text            = CharSequence
  override type Pong            = Unit
  override type InternalContext = JdkInternalContext.type

  override lazy val implicits = new JdkImplcits with Implicits
  case object JdkInternalContext
  case class JdkOptions(
    override val headers: Map[String, String] = Map.empty,
    override val subProtocol: Option[String] = None,
    sslCtx: Option[SSLContext] = None
  ) extends CommonOptions

  override type Options = JdkOptions

  override def defaultOptions: JdkOptions = JdkOptions()
}

object JdkPlatform extends JdkPlatform
