package com.github.andyglow.websocket

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util.HexFormat

object PekkoPlay {

  def main(args: Array[String]): Unit = {
    val p: Platform = PekkoPlatform
    import p._
    import p.implicits._

    val cli = p.newClient(ServerAddress("ws://localhost:9098/websocket"))
    import cli._

//    val pf = p.WebsocketHandler.Builder({
//      case p.Msg.Str("bar")       => println("got text `bar`")
//      case p.Msg.Str(someString)  => println(s"got text `$someString`")
//      case p.Msg.ByteArr(arr)     => println(s"got bytes [${HexFormat.of().formatHex(arr)}]")
//      case p.Msg.Pong()           => println(s"got pong")
//    }).build()
//    val ws = cli.open(pf)

//    val ws = cli.open({
//      case p.Msg.Str("bar")       => println("got text `bar`")
//      case p.Msg.Str(someString)  => println(s"got text `$someString`")
//      case p.Msg.ByteArr(arr)     => println(s"got bytes [${HexFormat.of().formatHex(arr)}]")
//      case p.Msg.Pong()           => println(s"got pong")
//    })

    val ws = cli.open(p.onMessage {
      case M.String("bar")      => println("got atom `bar`")
      case M.String(someString) => println(s"got string `$someString`")
      case M.`Array[Char]`(arr) => println(s"got char arr `${new String(arr)}`")
      case M.`Array[Byte]`(arr) =>
        println(s"got byte arr [${arr.length} ${HexFormat.of().formatHex(arr)}|${new String(arr)}]")
      case M.ByteBuffer(buf) =>
        println(
          s"got byte buf [${buf.capacity()} ${HexFormat.of().formatHex(buf.asByteArray)}}|${new String(buf.asByteArray)}]"
        )
    }.onUnhandled { case x =>
      println(s"got unhandled $x")
    }.onFailure { case x: Exception =>
      println(s"got exception"); x.printStackTrace()
    })

    ws.ping() // expect unhandled Pong

    ws.send("bar") // atom
    ws.send("string")
    ws.send("byte array".getBytes)
    ws.send("char array".toCharArray)
    ws.send(CharBuffer.wrap("char buffer"))
    ws.send(ByteBuffer.wrap("byte buffer".getBytes))

    Thread.sleep(1000L)
    cli.shutdown()
  }
}
