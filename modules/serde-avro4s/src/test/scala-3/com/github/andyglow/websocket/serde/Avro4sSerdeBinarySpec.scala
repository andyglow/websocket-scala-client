package com.github.andyglow.websocket.serde

import com.sksamuel.avro4s.Decoder
import com.sksamuel.avro4s.Encoder
import com.sksamuel.avro4s.SchemaFor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class Avro4sSerdeBinarySpec extends AnyFunSuite {
  import Models.*

  implicit val platform: TestPlatform = new TestPlatform
  import platform._

//  import Encoder.given
//  import Decoder.given
//  import SchemaFor.given

  val serde = new Avro4SSerde
  import serde._
  import serde.binary.given_MessageAdapter_T

  val cli = platform.newClient()
  import cli._

  test("send.json: primitives") {
    val h = new MockedWebsocketHandler
    val s = cli.open(h)
//    s.send(128)
    s.send("foo")
//    s.send(3.14)
//    s.send(false)
//    h.handledMessages should have size (4)
//    h.handledMessages(0) shouldBe Msg.Binary(Array[Byte](-128, 2))
//    h.handledMessages(1) shouldBe Msg.Binary(Array[Byte](6, 102, 111, 111))
//    h.handledMessages(2) shouldBe Msg.Binary(Array[Byte](31, -123, -21, 81, -72, 30, 9, 64))
//    h.handledMessages(3) shouldBe Msg.Binary(Array[Byte](0))
  }

//  test("send.json: little struct") {
//    val h = new MockedWebsocketHandler
//    val s = cli.open(h)
//    s.send(NestedEntry("id", 34.76))
//    h.handledMessages should have size (1)
//    h.handledMessages.head shouldBe Msg.Binary(Array[Byte](4, 105, 100, -31, 122, 20, -82, 71, 97, 65, 64))
//  }
}
