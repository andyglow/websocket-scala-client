package com.github.andyglow.websocket.serde

import com.github.andyglow.websocket.Platform
import com.sksamuel.avro4s.AvroInputStream
import com.sksamuel.avro4s.AvroOutputStream
import com.sksamuel.avro4s.Decoder
import com.sksamuel.avro4s.Encoder
import com.sksamuel.avro4s.SchemaFor
import java.io.ByteArrayOutputStream

class Avro4SSerde {
  import java.nio.charset.StandardCharsets.UTF_8

  object binary {

    implicit def binaryAvro4sMessageAdapter[T](implicit
      p: Platform,
      enc: Encoder[T],
      dec: Decoder[T],
      sf: SchemaFor[T]
    ): p.MessageAdapter[T] =
      new p.MessageAdapter[T] {
        override type F = p.Binary

        override def toMessage(x: T)(implicit ic: p.InternalContext): p.Binary = {
          val os  = new ByteArrayOutputStream
          val out = AvroOutputStream.binary[T].to(os).build()
          try out.write(Seq(x))
          finally out.close()
          p.implicits.ByteArrayMessageAdapter.toMessage(os.toByteArray)
        }

        override def fromMessage(x: p.Binary)(implicit ic: p.InternalContext): T = {
          val bytes = p.implicits.ByteArrayMessageAdapter.fromMessage(x)
          val in    = AvroInputStream.binary[T].from(bytes).build(sf.schema)
          in.iterator.next()
        }
      }
  }

  object json {

    implicit def jsonAvro4sMessageAdapter[T](implicit
      p: Platform,
      enc: Encoder[T],
      dec: Decoder[T],
      sf: SchemaFor[T]
    ): p.MessageAdapter[T] =
      new p.MessageAdapter[T] {
        override type F = p.Text

        override def toMessage(x: T)(implicit ic: p.InternalContext): p.Text = {
          val os  = new ByteArrayOutputStream
          val out = AvroOutputStream.json[T].to(os).build()
//          val out = AvroOutputStream.json(enc.withSchema(sf)).to(os).build()
          try out.write(Seq(x))
          finally out.close()
          p.implicits.StringMessageAdapter.toMessage(new String(os.toByteArray, UTF_8))
        }

        override def fromMessage(x: p.Text)(implicit ic: p.InternalContext): T = {
          val text = p.implicits.StringMessageAdapter.fromMessage(x)
//          val in = AvroInputStream.json(dec.withSchema(sf)).from(text).build(sf.schema)
          val in = AvroInputStream.json[T].from(text).build(sf.schema)
          in.iterator.next()
        }
      }
  }
}
