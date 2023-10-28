package com.github.andyglow.websocket

import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.OptionValues._
import java.net.URI

class ServerAddressSpec extends AnyFunSuite {

  val address = ServerAddress(
    secure = true,
    "localhost",
    8443,
    Some("/path"),
    Map(
      "foo" -> "bar",
      "bar" -> "baz"
    )
  )

  val uri = address.build()

  test("scheme") {
    address.scheme shouldBe "wss"
    uri.getScheme shouldBe "wss"
  }

  test("path") {
    uri.getPath shouldBe "/path"
  }

  test("query") {
    uri.getQuery shouldBe "foo=bar&bar=baz"
  }

  test("fromUri") {
    val address = ServerAddress.apply(URI.create("ws://localhost:8080/path?foo=bar&bar=baz"))
    address.secure shouldBe false
    address.scheme shouldBe "ws"
    address.host shouldBe "localhost"
    address.port shouldBe 8080
    address.path.value shouldBe "/path"
    address.query shouldBe Map(
      "foo" -> "bar",
      "bar" -> "baz"
    )
  }
}
