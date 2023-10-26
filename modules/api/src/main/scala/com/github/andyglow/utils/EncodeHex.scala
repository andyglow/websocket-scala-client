package com.github.andyglow.utils

import java.nio.ByteOrder

// Borrowed from https://stackoverflow.com/questions/9655181/java-convert-a-byte-array-to-a-hex-string/58118078#58118078
object EncodeHex {

  private val LOOKUP_TABLE_LOWER =
    Array[Char](0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66)
  private val LOOKUP_TABLE_UPPER =
    Array[Char](0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46)

  def apply(
    bytes: Array[Byte],
    maxLen: Int = 16,
    upperCase: Boolean = true,
    byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
  ): String = {
    val buffer = Array.ofDim[Char](bytes.length * 2)
    val lookup = if (upperCase) LOOKUP_TABLE_UPPER else LOOKUP_TABLE_LOWER
    var index  = 0
    bytes.take(maxLen).zipWithIndex foreach { case (byte, i) =>
      index = if (byteOrder == ByteOrder.BIG_ENDIAN) i else bytes.length - i - 1
      buffer(i << 1) = lookup((byte >> 4) & 0xf)
      buffer((i << 1) + 1) = lookup(byte & 0xf)
    }
    val result = new String(buffer)
    if (maxLen < bytes.length) result + " ..." else result
  }
}
