package com.github.andyglow.utils

object EncodeHex {

  def apply(x: Byte, xs: Byte*): String = apply(x +: xs.toArray)

  def apply(bytes: Array[Byte], maxLen: Int = 16): String = {
    val r = for { b <- bytes take maxLen } yield "%02X" format b
    if (bytes.length > maxLen) r.mkString("", " ", " ..") else r.mkString(" ")
  }
}
