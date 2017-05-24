package com.github.andyglow.websocket

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.handler.codec.http.websocketx.{BinaryWebSocketFrame, TextWebSocketFrame, WebSocketFrame}

sealed abstract class MessageFormat[T](val format: T => WebSocketFrame)

trait LowPriorityMessageFormats {
  implicit case object String extends MessageFormat[String](new TextWebSocketFrame(_))
  implicit case object CharArr extends MessageFormat[Array[Char]](x => new TextWebSocketFrame(new String(x)))

  implicit case object ByteBuf extends MessageFormat[ByteBuf](new BinaryWebSocketFrame(_))
  implicit case object ByteArr extends MessageFormat[Array[Byte]](x => new BinaryWebSocketFrame(Unpooled.wrappedBuffer(x)))
}

object MessageFormat extends LowPriorityMessageFormats
