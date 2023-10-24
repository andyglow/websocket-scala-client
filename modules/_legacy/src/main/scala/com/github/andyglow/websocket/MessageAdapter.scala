package com.github.andyglow.websocket

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import java.nio.ByteBuffer
import java.nio.CharBuffer

/** Scala types can't be used as message types. For instance you can't use strings, byte arrays out of the box. On order
  * to be sent to netty pipelines the message type needs to be adapted to one of ByteBufHolder implementors. So,
  * MessageAdapter is a type-class for Scala to Netty (and back) type adaptation. As an outcome you see adapted message
  * and corresponding Netty type as [[MessageAdapter.Frame]].
  *
  * Today we support these type conversion:
  *   - [x] String <-> TextWebSocketFrame
  *   - [x] Array[Char] <-> TextWebSocketFrame
  *   - [x] CharBuffer <-> TextWebSocketFrame
  *   - [x] Array[Byte] <-> BinaryWebSocketFrame
  *   - [x] ByteBuf <-> BinaryWebSocketFrame
  *   - [x] ByteBuffer <-> BinaryWebSocketFrame
  *
  * @tparam T
  *   Scala type
  */
sealed trait MessageAdapter[T] {

  type Frame <: ByteBufHolder

  /** Convert from Scala's message representation to Netty's
    * @param msg
    *   Scala's message
    * @return
    *   Message
    */
  def format(msg: T): Frame

  /** Convert from Netty's message representation to Scala's
    * @param msg
    *   Netty's message
    * @return
    *   Message
    */
  def parse(msg: ByteBufHolder): Option[T]
}

trait LowPriorityMessageFormats {
  type Aux[T, F] = MessageAdapter[T] { type Frame = F }
  import PartialFunction._

  implicit val StringMA: Aux[String, TextWebSocketFrame] = new MessageAdapter[String] {

    override type Frame = TextWebSocketFrame

    override def format(msg: String): Frame = new TextWebSocketFrame(msg)

    override def parse(msg: ByteBufHolder): Option[String] = condOpt(msg) { case msg: TextWebSocketFrame =>
      msg.text()
    }
  }

  implicit val CharArrMA: Aux[Array[Char], TextWebSocketFrame] = new MessageAdapter[Array[Char]] {

    override type Frame = TextWebSocketFrame

    override def format(msg: Array[Char]): Frame = new TextWebSocketFrame(new String(msg))

    override def parse(msg: ByteBufHolder): Option[Array[Char]] = condOpt(msg) { case msg: TextWebSocketFrame =>
      msg.text().toCharArray
    }
  }

  implicit val CharBufferMA: Aux[CharBuffer, TextWebSocketFrame] = new MessageAdapter[CharBuffer] {

    override type Frame = TextWebSocketFrame

    override def format(msg: CharBuffer): Frame = new TextWebSocketFrame(msg.toString)

    override def parse(msg: ByteBufHolder): Option[CharBuffer] = condOpt(msg) { case msg: TextWebSocketFrame =>
      CharBuffer.wrap(msg.text().toCharArray)
    }
  }

  implicit val ByteBufMA: Aux[ByteBuf, BinaryWebSocketFrame] = new MessageAdapter[ByteBuf] {

    override type Frame = BinaryWebSocketFrame

    override def format(msg: ByteBuf): Frame = new BinaryWebSocketFrame(msg)

    override def parse(msg: ByteBufHolder): Option[ByteBuf] = condOpt(msg) { case msg: BinaryWebSocketFrame =>
      msg.content()
    }
  }

  implicit val ByteBufferMA: Aux[ByteBuffer, BinaryWebSocketFrame] = new MessageAdapter[ByteBuffer] {

    override type Frame = BinaryWebSocketFrame

    override def format(msg: ByteBuffer): Frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg))

    override def parse(msg: ByteBufHolder): Option[ByteBuffer] = condOpt(msg) { case msg: BinaryWebSocketFrame =>
      msg.content().nioBuffer()
    }
  }

  implicit val ByteArrMA: Aux[Array[Byte], BinaryWebSocketFrame] = new MessageAdapter[Array[Byte]] {

    override type Frame = BinaryWebSocketFrame

    override def format(msg: Array[Byte]): Frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg))

    override def parse(msg: ByteBufHolder): Option[Array[Byte]] = condOpt(msg) { case msg: BinaryWebSocketFrame =>
      msg.content().array()
    }
  }
}

object MessageAdapter extends LowPriorityMessageFormats {

  def apply[T: MessageAdapter]: MessageAdapter[T] = implicitly
}
