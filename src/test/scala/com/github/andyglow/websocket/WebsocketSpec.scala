package com.github.andyglow.websocket

import io.netty.buffer.ByteBufHolder
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util.concurrent.Executors
import org.mockito.Mockito._
import org.scalactic.source.Position
import org.scalatest.funsuite.AnyFunSuite
import scala.concurrent.ExecutionContext

class WebsocketSpec extends AnyFunSuite {

  test("send/text") {
    val text = new TextWebSocketFrame("foo");
    examineSend("foo", text)
    examineSend("foo".toCharArray, text)
    examineSend(CharBuffer.wrap("foo"), text)
  }

  test("send/binary") {
    val binary = new BinaryWebSocketFrame(Unpooled.wrappedBuffer("foo".getBytes()))
    examineSend("foo".getBytes, binary)
    examineSend(ByteBuffer.wrap("foo".getBytes()), binary)
    examineSend(Unpooled.wrappedBuffer("foo".getBytes()), binary)
  }

  test("ping") {
    examine(
      ws => ws.ping(),
      ch => verify(ch).writeAndFlush(new PingWebSocketFrame)
    )
  }

  test("close") {
    examine(
      ws => ws.close(),
      ch => verify(ch).writeAndFlush(new CloseWebSocketFrame)
    )
  }

  private def examine(wsRun: Websocket => Any, verifyRun: Channel => Any)(implicit pos: Position): Unit = {
    val ch       = mock(classOf[Channel])
    val closeFut = mock(classOf[ChannelFuture])
    when(ch.closeFuture()).thenReturn(closeFut)

    val ws = new WebsocketImpl(ch)
    wsRun(ws)
    try {
      verifyRun(ch)
      ()
    } catch {
      case th: Throwable => fail(th)
    }
  }

  private def examineSend[T: MessageAdapter](msg: T, expectation: ByteBufHolder)(implicit pos: Position): Unit = {
    examine(
      ws => ws ! msg,
      ch => verify(ch).writeAndFlush(expectation)
    )
  }

  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(
    Executors.newSingleThreadExecutor()
  )
}
