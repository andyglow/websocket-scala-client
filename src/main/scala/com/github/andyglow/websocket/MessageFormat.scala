package com.github.andyglow.websocket

import io.netty.buffer.ByteBuf

sealed trait MessageFormat[T]
object MessageFormat {
  implicit case object String extends MessageFormat[String]
  implicit case object ByteBuf extends MessageFormat[ByteBuf]
}
