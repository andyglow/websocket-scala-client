package com.github.andyglow.websocket.serde

import com.github.andyglow.websocket.Platform
import com.sksamuel.avro4s._
import java.io.ByteArrayOutputStream

class Avro4SSerde {
  import java.nio.charset.StandardCharsets.UTF_8

  object binary {
    given [T](using p: Platform, enc: Encoder[T], dec: Decoder[T], sf: SchemaFor[T]): p.MessageAdapter[T] =
      new p.MessageAdapter[T] {
        override type F = p.Binary
        override def toMessage(x: T)(using ic: p.InternalContext): p.Binary =
          p.implicits.ByteArrayMessageAdapter.toMessage(Avro4SHelper.messageToByteArray(x))
        override def fromMessage(x: p.Binary)(using ic: p.InternalContext): T =
          Avro4SHelper.byteArrayToMessage(p.implicits.ByteArrayMessageAdapter.fromMessage(x))
      }
  }

  object json {
    given [T](using p: Platform, enc: Encoder[T], dec: Decoder[T], sf: SchemaFor[T]): p.MessageAdapter[T] =
      new p.MessageAdapter[T] {
        override type F = p.Text
        override def toMessage(x: T)(using ic: p.InternalContext): p.Text =
          p.implicits.StringMessageAdapter.toMessage(Avro4SHelper.messageToJsonString(x))
        override def fromMessage(x: p.Text)(using ic: p.InternalContext): T =
          Avro4SHelper.jsonStringToMessage(p.implicits.StringMessageAdapter.fromMessage(x))
      }
  }
}
