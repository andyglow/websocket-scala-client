package com.github.andyglow.utils

import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

class EncodeHexSpec extends AnyFunSuite {
  implicit def i_to_b(x: Int): Byte = x.toByte

  test("atoms") {
    forAll { b: Byte =>
      EncodeHex(Array(b)) shouldBe String.format("%02X", b);
    }
  }

  test("regular cases") {
    EncodeHex(Array.empty[Byte]) shouldBe empty

    EncodeHex(0xFA) shouldBe "FA"
    EncodeHex(0xFA, 0xAC) shouldBe "FA AC"

    EncodeHex(0x00, 0x01) shouldBe "00 01"
    EncodeHex(0x00, 0x01, 0x02) shouldBe "00 01 02"
    EncodeHex(0x00, 0x01, 0x02, 0x03) shouldBe "00 01 02 03"
  }

  test("long arrays") {
    val arr = (0x00 to 0x32).map(_.byteValue).toArray
    EncodeHex(arr) shouldBe "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F .."
  }
}
