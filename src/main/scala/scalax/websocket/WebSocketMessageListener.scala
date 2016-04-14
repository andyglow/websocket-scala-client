package scalax.websocket

import io.netty.buffer.ByteBuf

trait WebSocketMessageListener {
  def onText(text: String): Unit
  def onBinary(binary: ByteBuf): Unit
}

object WebSocketMessageListener {

  def textOnly(t: String => Unit): WebSocketMessageListener = new WebSocketMessageListener {
    override def onText(text: String): Unit = t(text)
    override def onBinary(binary: ByteBuf): Unit = ()
  }

  def binaryOnly(b: ByteBuf => Unit): WebSocketMessageListener = new WebSocketMessageListener {
    override def onText(text: String): Unit = ()
    override def onBinary(binary: ByteBuf): Unit = b(binary)
  }

  def apply(t: String => Unit)(b: ByteBuf => Unit): WebSocketMessageListener = new WebSocketMessageListener {
    override def onText(text: String): Unit = t(text)
    override def onBinary(binary: ByteBuf): Unit = b(binary)
  }

}
