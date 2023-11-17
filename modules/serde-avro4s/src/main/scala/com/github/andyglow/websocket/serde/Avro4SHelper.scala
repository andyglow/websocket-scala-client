package com.github.andyglow.websocket.serde

import com.sksamuel.avro4s.AvroInputStream
import com.sksamuel.avro4s.AvroOutputStream
import com.sksamuel.avro4s.Decoder
import com.sksamuel.avro4s.Encoder
import com.sksamuel.avro4s.SchemaFor
import java.io.ByteArrayOutputStream

private[serde] object Avro4SHelper {
  import java.nio.charset.StandardCharsets.UTF_8

  def messageToByteArray[T](x: T)(implicit enc: Encoder[T], sf: SchemaFor[T]): Array[Byte] = {
    val os  = new ByteArrayOutputStream
    val out = AvroOutputStream.binary[T].to(os).build()
    try out.write(Seq(x))
    finally out.close()
    os.toByteArray
  }

  def byteArrayToMessage[T](bytes: Array[Byte])(implicit enc: Decoder[T], sf: SchemaFor[T]): T = {
    val in = AvroInputStream.binary[T].from(bytes).build(sf.schema)
    in.iterator.next()
  }

  def messageToJsonString[T](x: T)(implicit enc: Encoder[T], sf: SchemaFor[T]): String = {
    val os  = new ByteArrayOutputStream
    val out = AvroOutputStream.json[T].to(os).build()
    try out.write(Seq(x))
    finally out.close()
    new String(os.toByteArray, UTF_8)
  }

  def jsonStringToMessage[T](json: String)(implicit enc: Decoder[T], sf: SchemaFor[T]): T = {
    val in = AvroInputStream.json[T].from(json).build(sf.schema)
    in.iterator.next()
  }
}
