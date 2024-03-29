package com.github.andyglow.websocket

import java.nio.ByteBuffer
import java.nio.CharBuffer

trait JdkImplicits { this: JdkPlatform =>

  class JdkImplcits extends MessageAdapter.Implicits {

    private class TextAdapter[T <: CharSequence](ff: Text => T) extends MessageAdapter[T] {
      override type F = Text
      override def toMessage(msg: T)(implicit ic: InternalContext): F   = msg
      override def fromMessage(msg: F)(implicit ic: InternalContext): T = ff(msg)
    }

    override implicit val StringMessageAdapter: MessageAdapter.Aux[String, Text] =
      new TextAdapter[String](_.toString)

    override implicit val CharBufferMessageAdapter: MessageAdapter.Aux[CharBuffer, Text] =
      new TextAdapter[CharBuffer](m => CharBuffer.wrap(m.toString))

    implicit val CharArrayMessageAdapter: MessageAdapter.Aux[Array[Char], Text] = new MessageAdapter[Array[Char]] {
      override type F = Text
      override def toMessage(msg: Array[Char])(implicit ic: InternalContext): F      = CharBuffer.wrap(msg)
      override def fromMessage(msg: Text)(implicit ic: InternalContext): Array[Char] = msg.toString.toCharArray
    }

    implicit val ByteBufferMessageAdapter: MessageAdapter.Aux[ByteBuffer, Binary] = new MessageAdapter[ByteBuffer] {
      override type F = Binary
      override def toMessage(msg: ByteBuffer)(implicit ic: InternalContext): F        = msg
      override def fromMessage(msg: Binary)(implicit ic: InternalContext): ByteBuffer = msg
    }

    implicit val ByteArrayMessageAdapter: MessageAdapter.Aux[Array[Byte], Binary] = new MessageAdapter[Array[Byte]] {
      override type F = Binary
      override def toMessage(msg: Array[Byte])(implicit ic: InternalContext): F        = ByteBuffer.wrap(msg)
      override def fromMessage(msg: Binary)(implicit ic: InternalContext): Array[Byte] = msg.asByteArray
    }
  }

  override protected def cast[T](
    x: MessageType,
    onBinary: Binary => T,
    onText: Text => T,
    onPong: => T
  ): T = x match {
    case frame: Binary => onBinary(frame)
    case frame: Text   => onText(frame)
    case frame: Pong   => onPong
    case _             => throw new IllegalArgumentException(s"unsupported message type: ${x.getClass}")
  }
}
