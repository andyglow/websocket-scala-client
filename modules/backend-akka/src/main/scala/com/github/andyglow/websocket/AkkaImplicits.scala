package com.github.andyglow.websocket

import java.nio.ByteBuffer
import java.nio.CharBuffer
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.Materializer
import akka.util.ByteString
import scala.concurrent.Await

trait AkkaImplicits { this: AkkaPlatform =>

  class PekkoImplicits(options: AkkaOptions)(implicit val mat: Materializer)
      extends MessageAdapter.Implicits
      with Implicits {
    import mat.executionContext

    private class TextBridge[T](
      toString: T => String,
      fromString: String => T
    ) extends MessageAdapter[T] {
      override type F = Text
      override def toMessage(msg: T): F = ws.TextMessage(toString(msg))
      override def fromMessage(msg: F): T =
        Await.result(
          msg.toStrict(options.readStreamedMessageTimeout)(mat).map(_.text).map(fromString),
          options.resolveTimeout
        )
    }

    private class BinaryBridge[T](
      toByteString: T => ByteString,
      fromByteString: ByteString => T
    ) extends MessageAdapter[T] {
      override type F = Binary
      override def toMessage(msg: T): F = ws.BinaryMessage(toByteString(msg))
      override def fromMessage(msg: F): T =
        Await.result(
          msg.toStrict(options.readStreamedMessageTimeout)(mat).map(_.data).map(fromByteString),
          options.resolveTimeout
        )
    }

    override implicit val StringMessageAdapter: MessageAdapter.Aux[String, Text] =
      new TextBridge(
        toString = identity,
        fromString = identity
      )

    override implicit val CharBufferMessageAdapter: MessageAdapter.Aux[CharBuffer, Text] =
      new TextBridge(
        toString = _.toString,
        fromString = CharBuffer.wrap
      )

    override implicit val CharArrayMessageAdapter: MessageAdapter.Aux[Array[Char], Text] =
      new TextBridge(
        toString = new String(_),
        fromString = _.toCharArray
      )

    implicit val ByteBufferMessageAdapter: MessageAdapter.Aux[ByteBuffer, Binary] =
      new BinaryBridge(
        toByteString = ByteString.apply,
        fromByteString = _.asByteBuffer
      )

    implicit val ByteArrayMessageAdapter: MessageAdapter.Aux[Array[Byte], BinaryMessage] =
      new BinaryBridge(
        toByteString = ByteString.apply,
        fromByteString = _.asByteBuffer.toByteArray
      )

  }

  override protected def cast[T](
    x: MessageType,
    onBinary: Binary => T,
    onText: Text => T,
    onPong: => T
  ): T = x match {
    case frame: Binary => onBinary(frame)
    case frame: Text   => onText(frame)
    case _             => throw new IllegalArgumentException(s"unsupported message type: ${x.getClass}")
  }
}
