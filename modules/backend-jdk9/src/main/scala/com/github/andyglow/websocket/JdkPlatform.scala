package com.github.andyglow.websocket

import com.github.andyglow.utils.EncodeHex
import java.nio.ByteBuffer
import javax.net.ssl.SSLContext
import scala.concurrent.duration._

class JdkPlatform extends Platform with JdkImplicits with JdkClient {
  override type MessageType     = Any
  override type Binary          = ByteBuffer
  override type Text            = CharSequence
  override type Pong            = Unit
  override type InternalContext = JdkInternalContext.type

  override val implicits = new JdkImplcits with Implicits
  case object JdkInternalContext
  case class JdkOptions(
    override val headers: Map[String, String] = Map.empty,
    override val subProtocol: Option[String] = None,
    override val tracer: Tracer = acceptingAnyPF(()),
    sslCtx: Option[SSLContext] = None,
    futureResolutionTimeout: FiniteDuration = 2.seconds
  ) extends CommonOptions {
    override def withTracer(tracer: Tracer): JdkOptions = copy(tracer = tracer)
  }

  override type Options = JdkOptions

  override def defaultOptions: JdkOptions = JdkOptions()

  override protected def stringify(x: Any): String = x match {
    case x: Text   => x.toString
    case x: Binary => EncodeHex(x.asByteArray)
    case _         => s"unknown($x)"
  }

  protected implicit def optionFromOptional[T](x: java.util.Optional[T]): Option[T] = {
    if (x.isPresent) Some(x.get()) else None
  }
}

object JdkPlatform extends JdkPlatform
