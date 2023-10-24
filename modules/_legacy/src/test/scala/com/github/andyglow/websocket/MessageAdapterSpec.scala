package com.github.andyglow.websocket

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.handler.codec.http.websocketx.{BinaryWebSocketFrame, TextWebSocketFrame}
import org.scalatest.matchers.should.Matchers._
import org.scalatest.OptionValues._
import org.scalatest.wordspec.AnyWordSpec

import java.nio.CharBuffer

class MessageAdapterSpec extends AnyWordSpec {

  "MessageAdapter" should {

    "support Strings" in {
      MessageAdapter[String].format("foo") shouldEqual new TextWebSocketFrame("foo")
      MessageAdapter[String].parse(new TextWebSocketFrame("foo")).value shouldEqual "foo"
    }

    "support char arrays" in {
      MessageAdapter[Array[Char]].format("foo".toCharArray) shouldEqual new TextWebSocketFrame("foo")
      MessageAdapter[Array[Char]].parse(new TextWebSocketFrame("foo")).value shouldEqual "foo".toCharArray
    }

    "support char buffer" in {
      MessageAdapter[CharBuffer].format(CharBuffer.wrap("foo")) shouldEqual new TextWebSocketFrame("foo")
      MessageAdapter[CharBuffer].parse(new TextWebSocketFrame("foo")).value shouldEqual CharBuffer.wrap("foo")
    }

    "support byte arrays" in {
      MessageAdapter[Array[Byte]].format("foo".getBytes) shouldEqual new BinaryWebSocketFrame(Unpooled.wrappedBuffer("foo".getBytes))
      MessageAdapter[Array[Byte]].parse(new BinaryWebSocketFrame(Unpooled.wrappedBuffer("foo".getBytes))).value shouldEqual "foo".getBytes
    }

    "support byte buf" in {
      MessageAdapter[ByteBuf].format(Unpooled.wrappedBuffer("foo".getBytes)) shouldEqual new BinaryWebSocketFrame(Unpooled.wrappedBuffer("foo".getBytes))
      MessageAdapter[ByteBuf].parse(new BinaryWebSocketFrame(Unpooled.wrappedBuffer("foo".getBytes))).value shouldEqual Unpooled.wrappedBuffer("foo".getBytes)
    }
  }
}
