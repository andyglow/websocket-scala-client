package com.github.andyglow.websocket.serde

import com.github.andyglow.websocket.Platform
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, Encoder, SchemaFor}

import java.io.ByteArrayOutputStream

class Avro4SSerde(implicit val platform: Platform) {
  import platform._
  import java.nio.charset.StandardCharsets.UTF_8

  object binary {

    implicit def binaryAvro4sMessageAdapter[T](implicit enc: Encoder[T], sf: SchemaFor[T]): MessageAdapter.Aux[T, Binary] =
      new MessageAdapter[T] {
        override type F = Binary

        override def toMessage(x: T)(implicit ic: InternalContext): Binary   = {
          val os = new ByteArrayOutputStream
          val out = AvroOutputStream.binary[T].to(os).build()
          try out.write(Seq(x)) finally out.close()
          platform.implicits.ByteArrayMessageAdapter.toMessage(os.toByteArray)
        }

        override def fromMessage(x: Binary)(implicit ic: InternalContext): T = {
          val bytes = platform.implicits.ByteArrayMessageAdapter.fromMessage(x)
          val in = AvroInputStream.binary[T].from(bytes).build(sf.schema)
          in.iterator.next()
        }
      }
  }

  object json {

    implicit def jsonAvro4sMessageAdapter[T](implicit enc: Encoder[T], sf: SchemaFor[T]): MessageAdapter.Aux[T, Text] =
      new MessageAdapter[T] {
        override type F = Text

        override def toMessage(x: T)(implicit ic: InternalContext): Text   = {
          val os = new ByteArrayOutputStream
          val out = AvroOutputStream.json[T].to(os).build()
          try out.write(Seq(x)) finally out.close()
          platform.implicits.StringMessageAdapter.toMessage(new String(os.toByteArray, UTF_8))
        }

        override def fromMessage(x: Text)(implicit ic: InternalContext): T = {
          val text = platform.implicits.StringMessageAdapter.fromMessage(x)
          val in = AvroInputStream.json[T].from(text).build(sf.schema)
          in.iterator.next()
        }
      }
  }
}
