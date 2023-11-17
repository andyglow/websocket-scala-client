package com.github.andyglow.websocket.serde

import com.github.andyglow.websocket.Platform
import com.github.plokhotnyuk.jsoniter_scala.core._

class JsoniterScalaSerde {

  implicit def jsoniterScalaMessageAdapter[T](implicit
    p: Platform,
    codec: JsonValueCodec[T]
  ): p.MessageAdapter[T] = new p.MessageAdapter[T] {
    override type F = p.Text
    override def toMessage(x: T)(implicit ic: p.InternalContext): F = {
      val str = writeToString(x)
      p.implicits.StringMessageAdapter.toMessage(str)
    }
    override def fromMessage(x: F)(implicit ic: p.InternalContext): T = {
      val str = p.implicits.StringMessageAdapter.fromMessage(x)
      readFromString(str)
    }
  }
}

object JsoniterScalaSerde extends JsoniterScalaSerde
