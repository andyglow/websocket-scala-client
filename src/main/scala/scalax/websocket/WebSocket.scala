package scalax.websocket

import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelFuture}
import io.netty.handler.codec.http.websocketx._

class WebSocket(ch: Channel) {

  def sendText(msg: String): ChannelFuture = ch.writeAndFlush(new TextWebSocketFrame(msg))
  def sendBinary(msg: ByteBuf): ChannelFuture = ch.writeAndFlush(new BinaryWebSocketFrame(msg))

  def close(): ChannelFuture = {
    ch.writeAndFlush(new CloseWebSocketFrame())
    ch.closeFuture()
  }

}
