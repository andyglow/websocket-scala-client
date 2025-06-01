package com.github.andyglow.websocket

import com.github.andyglow.utils.EncodeHex
import java.nio.ByteBuffer
import java.nio.CharBuffer
import scala.collection.mutable.ListBuffer

class TestPlatform extends Platform {

  trait Msg
  object Msg {
    case class Text(value: String)        extends Msg
    case class Binary(value: Array[Byte]) extends Msg {
      override def equals(obj: Any): Boolean = obj match {
        case Binary(other) => java.util.Arrays.equals(value, other)
        case _             => false
      }
      override def toString: String = s"Binary(${EncodeHex(value)})"
    }
    case object Pong extends Msg
  }

  override type MessageType = Msg
  override type Binary      = Msg.Binary
  override type Text        = Msg.Text
  override type Pong        = Msg.Pong.type

  override type InternalContext = Unit

  override protected def cast[T](x: MessageType, ifBinary: Binary => T, ifText: Text => T, ifPong: => T): T = x match {
    case x: Binary => ifBinary(x)
    case x: Text   => ifText(x)
    case _         => throw new IllegalArgumentException()
  }

  override val implicits: MessageAdapter.Implicits with Implicits = new MessageAdapter.Implicits with Implicits {
    override implicit val StringMessageAdapter: MessageAdapter.Aux[String, Msg.Text] =
      MessageAdapter(Msg.Text.apply, _.value)
    override implicit val CharArrayMessageAdapter: MessageAdapter.Aux[Array[Char], Msg.Text] =
      MessageAdapter(x => Msg.Text(new String(x)), _.value.toCharArray)
    override implicit val CharBufferMessageAdapter: MessageAdapter.Aux[CharBuffer, Msg.Text] =
      MessageAdapter(x => Msg.Text(x.asString), x => CharBuffer.wrap(x.value))
    override implicit val ByteArrayMessageAdapter: MessageAdapter.Aux[Array[Byte], Msg.Binary] =
      MessageAdapter(Msg.Binary.apply, _.value)
    override implicit val ByteBufferMessageAdapter: MessageAdapter.Aux[ByteBuffer, Msg.Binary] =
      MessageAdapter(x => Msg.Binary(x.asByteArray), x => ByteBuffer.wrap(x.value))
  }

  override type Options = CommonOptions
  override def defaultOptions: CommonOptions = new CommonOptions {
    override def withTracer(tracer: Tracer): CommonOptions = this
  }

  def newClient(): WebsocketClient = newClient(null, null)
  override def newClient(address: ServerAddress, options: Options = defaultOptions): WebsocketClient =
    new WebsocketClient {
      override def open(handler: WebsocketHandler): Websocket = new Websocket {
        override protected def send(x: Msg): Unit =
          if (handler.onMessage.isDefinedAt(x)) handler.onMessage(x) else handler.onUnhandledMessage(x)
        override def ping(): Unit  = send(Msg.Pong)
        override def close(): Unit = handler.onClose(())
      }
      override def shutdown(): Unit  = ()
      override implicit val ic: Unit = ()
    }

  class MockedWebsocketHandler extends WebsocketHandler {
    val handledMessages: ListBuffer[Msg] = ListBuffer.empty
    override def onMessage: OnMessage    = { case x => handledMessages.append(x) }
  }

  override protected def stringify(x: Msg): String = x.toString
}
