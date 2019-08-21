package com.github.andyglow.websocket

import io.netty.buffer.{ByteBuf, ByteBufHolder, Unpooled}
import io.netty.handler.codec.http.websocketx.{BinaryWebSocketFrame, TextWebSocketFrame}


sealed trait MessageAdapter[T] {

  type Frame <: ByteBufHolder

  def format(msg: T): Frame

  def parse(msg: ByteBufHolder): Option[T]

  final def receive(handler: WebsocketHandler[T], msg: ByteBufHolder): Unit = {
    parse(msg) foreach { r =>
      if (handler.receive.isDefinedAt(r))
        handler.receive(r)
    }
  }
}

trait LowPriorityMessageFormats {
  type Aux[T, F] = MessageAdapter[T] { type Frame = F }
  import PartialFunction._

  implicit val String: Aux[String, TextWebSocketFrame] = new MessageAdapter[String] {

    override type Frame = TextWebSocketFrame

    override def format(msg: String): Frame = new TextWebSocketFrame(msg)

    override def parse(msg: ByteBufHolder): Option[String] = condOpt(msg) {
      case msg: TextWebSocketFrame => msg.text()
    }
  }

  implicit val CharArr: Aux[Array[Char], TextWebSocketFrame] = new MessageAdapter[Array[Char]] {

    override type Frame = TextWebSocketFrame

    override def format(msg: Array[Char]): Frame = new TextWebSocketFrame(new String(msg))

    override def parse(msg: ByteBufHolder): Option[Array[Char]] = condOpt(msg) {
      case msg: TextWebSocketFrame => msg.text().toCharArray
    }
  }

  implicit val ByteBuf: Aux[ByteBuf, BinaryWebSocketFrame] = new MessageAdapter[ByteBuf] {

    override type Frame = BinaryWebSocketFrame

    override def format(msg: ByteBuf): Frame = new BinaryWebSocketFrame(msg)

    override def parse(msg: ByteBufHolder): Option[ByteBuf] = condOpt(msg) {
      case msg: BinaryWebSocketFrame => msg.content()
    }
  }

  implicit val ByteArr: Aux[Array[Byte], BinaryWebSocketFrame] = new MessageAdapter[Array[Byte]] {

    override type Frame = BinaryWebSocketFrame

    override def format(msg: Array[Byte]): Frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg))

    override def parse(msg: ByteBufHolder): Option[Array[Byte]] = condOpt(msg) {
      case msg: BinaryWebSocketFrame => msg.content().array()
    }  }
}

object MessageAdapter extends LowPriorityMessageFormats
