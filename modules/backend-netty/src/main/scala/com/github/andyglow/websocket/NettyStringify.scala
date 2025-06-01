package com.github.andyglow.websocket

import com.github.andyglow.utils.EncodeHex
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame

private[websocket] object NettyStringify {

  def apply(x: Any): String = x match {
    case x: PongWebSocketFrame  => "pong"
    case x: CloseWebSocketFrame => "close"
    case x: TextWebSocketFrame  =>
      val s = x.text()
      x.content().retain()
      s"text[ $s ${if (x.isFinalFragment) "!" else ""}]"
    case x: BinaryWebSocketFrame =>
      s"binary[ ${NettyStringify(x.content())} ${if (x.isFinalFragment) "!" else ""}]"

    case x: ByteBuf =>
      val arr = Array.ofDim[Byte](x.capacity())
      x.getBytes(0, arr)
      x.retain()
      EncodeHex(arr)

    case _ => "unknown(" + x + ")"
  }
}
