package com.github.andyglow.websocket

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import java.nio.ByteBuffer
import java.nio.CharBuffer

trait NettyImplicits { this: NettyPlatform =>

  class NettyImplicits extends MessageAdapter.Implicits {
    import java.nio.charset.StandardCharsets.UTF_8

    private class TextAdapter[T <: CharSequence](ff: Text => T) extends MessageAdapter[T] {
      override type F = Text
      override def toMessage(msg: T)(implicit ic: InternalContext): F = new TextWebSocketFrame(
        Unpooled.copiedBuffer(msg, UTF_8)
      )
      override def fromMessage(msg: F)(implicit ic: InternalContext): T = ff(msg)
    }

    override implicit val StringMessageAdapter: MessageAdapter.Aux[String, Text] =
      new TextAdapter[String](_.text())

    override implicit val CharBufferMessageAdapter: MessageAdapter.Aux[CharBuffer, Text] =
      new TextAdapter[CharBuffer](m => CharBuffer.wrap(m.text()))

    implicit val CharArrayMessageAdapter: MessageAdapter.Aux[Array[Char], Text] = new MessageAdapter[Array[Char]] {
      override type F = Text
      override def toMessage(msg: Array[Char])(implicit ic: InternalContext): F = new TextWebSocketFrame(
        Unpooled.copiedBuffer(msg, UTF_8)
      )
      override def fromMessage(msg: TextWebSocketFrame)(implicit ic: InternalContext): Array[Char] =
        msg.text().toCharArray
    }

    implicit val ByteBufTB: MessageAdapter.Aux[ByteBuf, Binary] = new MessageAdapter[ByteBuf] {
      override type F = Binary
      override def toMessage(msg: ByteBuf)(implicit ic: InternalContext): F = new BinaryWebSocketFrame(msg)
      override def fromMessage(msg: BinaryWebSocketFrame)(implicit ic: InternalContext): ByteBuf = msg.content()
    }

    implicit val ByteBufferMessageAdapter: MessageAdapter.Aux[ByteBuffer, Binary] = new MessageAdapter[ByteBuffer] {
      override type F = Binary
      override def toMessage(msg: ByteBuffer)(implicit ic: InternalContext): F = new BinaryWebSocketFrame(
        Unpooled.wrappedBuffer(msg)
      )
      override def fromMessage(msg: BinaryWebSocketFrame)(implicit ic: InternalContext): ByteBuffer =
        msg.content().nioBuffer()
    }

    implicit val ByteArrayMessageAdapter: MessageAdapter.Aux[Array[Byte], Binary] = new MessageAdapter[Array[Byte]] {
      override type F = Binary
      override def toMessage(msg: Array[Byte])(implicit ic: InternalContext): F = new BinaryWebSocketFrame(
        Unpooled.wrappedBuffer(msg)
      )
      override def fromMessage(msg: BinaryWebSocketFrame)(implicit ic: InternalContext): Array[Byte] = {
        val c = msg.content()
        if (c.hasArray) c.array()
        else {
          val arr = Array.ofDim[Byte](c.readableBytes)
          c.getBytes(c.readerIndex, arr)
          arr
        }
      }
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
