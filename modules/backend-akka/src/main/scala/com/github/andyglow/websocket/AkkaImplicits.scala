package com.github.andyglow.websocket

import java.nio.ByteBuffer
import java.nio.CharBuffer
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.Materializer
import akka.util.ByteString
import scala.concurrent.Await

trait AkkaImplicits { this: AkkaPlatform =>

  class PekkoImplicits
      extends MessageAdapter.Implicits
      with Implicits {

    private class TextAdapter[T](
      toString: T => String,
      fromString: String => T
    ) extends MessageAdapter[T] {
      override type F = Text
      override def toMessage(msg: T)(implicit ic: InternalContext): F = ws.TextMessage(toString(msg))
      override def fromMessage(msg: F)(implicit ic: InternalContext): T = {
        import ic._
        import mat.executionContext

        Await.result(
          msg.toStrict(options.readStreamedMessageTimeout)(mat).map(_.text).map(fromString),
          options.resolveTimeout
        )
      }
    }

    private class BinaryAdapter[T](
      toByteString: T => ByteString,
      fromByteString: ByteString => T
    ) extends MessageAdapter[T] {
      override type F = Binary
      override def toMessage(msg: T)(implicit ic: InternalContext): F = ws.BinaryMessage(toByteString(msg))
      override def fromMessage(msg: F)(implicit ic: InternalContext): T = {
        import ic._
        import mat.executionContext

        Await.result(
          msg.toStrict(options.readStreamedMessageTimeout)(mat).map(_.data).map(fromByteString),
          options.resolveTimeout
        )
      }
    }

    override implicit val StringMessageAdapter: MessageAdapter.Aux[String, Text] =
      new TextAdapter(
        toString = identity,
        fromString = identity
      )

    override implicit val CharBufferMessageAdapter: MessageAdapter.Aux[CharBuffer, Text] =
      new TextAdapter(
        toString = _.toString,
        fromString = CharBuffer.wrap
      )

    override implicit val CharArrayMessageAdapter: MessageAdapter.Aux[Array[Char], Text] =
      new TextAdapter(
        toString = new String(_),
        fromString = _.toCharArray
      )

    implicit val ByteBufferMessageAdapter: MessageAdapter.Aux[ByteBuffer, Binary] =
      new BinaryAdapter(
        toByteString = ByteString.apply,
        fromByteString = _.asByteBuffer
      )

    implicit val ByteArrayMessageAdapter: MessageAdapter.Aux[Array[Byte], BinaryMessage] =
      new BinaryAdapter(
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
