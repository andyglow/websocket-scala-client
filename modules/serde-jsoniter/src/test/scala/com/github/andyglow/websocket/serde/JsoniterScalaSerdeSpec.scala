package com.github.andyglow.websocket.serde

import com.github.andyglow.websocket.TestModels._
import com.github.andyglow.websocket.TestPlatform
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite

class JsoniterScalaSerdeSpec extends AnyFunSuite {
  import JsoniterScalaSerdeSpec._
  implicit val platform: TestPlatform = new TestPlatform
  import platform._

  val serde: JsoniterScalaSerde = new JsoniterScalaSerde
  import serde._

  val cli = platform.newClient()
  import cli._

  test("send.json: primitives") {
    val h = new MockedWebsocketHandler
    val s = cli.open(h)
    s.send(128)
    s.send("foo")
    s.send(3.14)
    s.send(false)
    h.handledMessages should have size (4)
    h.handledMessages(0) shouldBe Msg.Text("128")
    h.handledMessages(1) shouldBe Msg.Text("\"foo\"")
    h.handledMessages(2) shouldBe Msg.Text("3.14")
    h.handledMessages(3) shouldBe Msg.Text("false")
  }

  test("send.json: little struct") {
    val h = new MockedWebsocketHandler
    val s = cli.open(h)
    s.send(NestedEntry("id", 34.76))
    h.handledMessages should have size (1)
    h.handledMessages.head shouldBe Msg.Text("{\"id\":\"id\",\"value\":34.76}")
  }

  test("send.json: larger struct") {
    val h = new MockedWebsocketHandler
    val s = cli.open(h)
    s.send(
      TestModel(
        id = "id",
        count = 16,
        series = List(12, 11.4, 0.123),
        nested = NestedTestModel("x501", 501L, active = true),
        seriesOfNested = List(NestedEntry("pi", 3.14))
      )
    )
    h.handledMessages should have size (1)
    h.handledMessages.head shouldBe Msg.Text(
      "{\"id\":\"id\",\"count\":16,\"series\":[12.0,11.4,0.123],\"nested\":{\"id\":\"x501\",\"value\":501,\"active\":true},\"seriesOfNested\":[{\"id\":\"pi\",\"value\":3.14}]}"
    )
  }
}

object JsoniterScalaSerdeSpec {
  import com.github.plokhotnyuk.jsoniter_scala.macros._

  implicit val intCodec: JsonValueCodec[Int] = JsonCodecMaker.make
  implicit val stringCodec: JsonValueCodec[String] = JsonCodecMaker.make
  implicit val doubleCodec: JsonValueCodec[Double] = JsonCodecMaker.make
  implicit val booleanCodec: JsonValueCodec[Boolean] = JsonCodecMaker.make
  implicit val testModelCodec: JsonValueCodec[TestModel] = JsonCodecMaker.make
  implicit val nestedEntryCodec: JsonValueCodec[NestedEntry] = JsonCodecMaker.make
  implicit def nestedTestModel[T: JsonValueCodec]: JsonValueCodec[NestedTestModel[T]] = JsonCodecMaker.make
}
