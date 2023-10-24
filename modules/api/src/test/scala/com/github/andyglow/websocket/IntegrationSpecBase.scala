package com.github.andyglow.websocket

import com.github.andyglow.websocket.server.WebsocketServer
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object IntegrationSpecBase {

  abstract class ServerHandle {
    def address: ServerAddress
    def shutdown(): Unit
  }

  protected class EmbeddedServer(ssl: Boolean) extends ServerHandle {
    protected final lazy val server = WebsocketServer.newServer(ssl)
    override def address: ServerAddress = {
      val scheme: String = if (ssl) "wss" else "ws"
      ServerAddress(s"$scheme://localhost:${server.port}${WebsocketServer.WebsocketPath}")
    }
    override def shutdown(): Unit = server.shutdown()
  }

  protected class Server(val address: ServerAddress) extends ServerHandle {
    override def shutdown(): Unit = ()
  }
}

trait IntegrationSpecBase extends AnyWordSpec
    with BeforeAndAfterAll {
  import IntegrationSpecBase._

  val platform: Platform

  def ssl: Boolean

  def isPingSupported: Boolean = true

  import platform._

  protected def options: Options = defaultOptions

  protected implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

//   protected final lazy val server: ServerHandle = new EmbeddedServer(ssl)
  protected final lazy val server: ServerHandle    = new Server(ServerAddress("ws://localhost:9098/websocket"))
  protected final lazy val client: WebsocketClient = platform.newClient(server.address, options)
  import client._
  import client.implicits._

  override def afterAll(): Unit = {
    client.shutdownSync()
    server.shutdown()
    super.afterAll()
  }

  // withState -> Builder0
  protected case class Builder0[T <: State](state: T) {
    def withHandler(handler: T => WebsocketHandler): Builder1[T] = Builder1(this, handler)
  }

  // withHandler -> Builder1
  protected case class Builder1[T <: State](parent: Builder0[T], handler: T => WebsocketHandler) {
    def withScript[O](script: Websocket => O) = Builder2(this, script)
  }

  // withScript -> Builder2
  protected case class Builder2[T <: State, O](parent: Builder1[T], script: Websocket => O) {
    def run(): (T, O) = {
      val ws = client.open(parent.handler(parent.parent.state))
      try { (parent.parent.state, script(ws)) }
      finally {
        parent.parent.state.whenFinished {
          Await.ready(ws.close(), 5.seconds)
        }
      }
    }
  }

  trait State {
    def whenFinished(fn: => Any): Unit
  }

  protected def withState[T <: State](init: T): Builder0[T] = Builder0(init)

  protected case class CountingDownState(count: Int = 1) extends State {
    val latch              = new CountDownLatch(count)
    def countDown(): Unit  = latch.countDown()
    def succeeded: Boolean = latch.getCount == 0
    def whenFinished(fn: => Any): Unit = {
      latch.await(2, TimeUnit.SECONDS)
      fn
    }
  }

  // test helper used to test single send
  private abstract class SingleSendRound[T: MessageAdapter](val value: T) {
    val Match: OnMessage
    lazy val (state, _) = withState(CountingDownState()).withHandler { latch =>
      onMessage {
        case m if Match.isDefinedAt(m) => latch.countDown()
        case m                         => fail(s"unexpected message: $m")
      }
    }.withScript { ws =>
      ws.send(value)
    }.run()
  }

  "Platform" when {

    s"connection is${if (ssl) "" else " not"} secured" should {

      "be able to" when {

        if (isPingSupported) {
          "ping/pong" in {
            val (state, _) = withState(CountingDownState()).withHandler { latch =>
              onMessage {
                case M.Pong() => latch.countDown()
                case m        => fail(s"unexpected message: $m")
              }
            }.withScript { ws =>
              ws.ping()
            }.run()

            assert(state.succeeded, "haven't been ponged in 1 second")
          }
        }

        "send/receive" when {

          "String" in new SingleSendRound("foo") {
            override val Match: OnMessage = { case M.String(`value`) => }
            assert(state.succeeded, "haven't received text echo in 1 second")
          }

          "Array[Char]" in new SingleSendRound("foo".toCharArray) {
            override val Match: OnMessage = { case M.`Array[Char]`(v) if util.Arrays.equals(v, value) => }
            assert(state.succeeded, "haven't received text echo in 1 second")
          }

          "Array[Byte]" in new SingleSendRound[Array[Byte]]("foo".getBytes) {
            override val Match: OnMessage = { case M.`Array[Byte]`(v) if util.Arrays.equals(v, value) => }
            assert(state.succeeded, "haven't received text echo in 1 second")
          }

          "CharBuffer" in new SingleSendRound(CharBuffer.wrap("foo")) {
            override val Match: OnMessage = { case M.CharBuffer(`value`) => }
            assert(state.succeeded, "haven't received text echo in 1 second")
          }

          "ByteBuffer" in new SingleSendRound(ByteBuffer.wrap("foo".getBytes)) {
            override val Match: OnMessage = { case M.ByteBuffer(v) if v.rewind() == value.rewind() => }
            assert(state.succeeded, "haven't received text echo in 1 second")
          }
        }
      }

      "support sender()" in {
        // client   server
        // --|---------|--
        // start ----->|
        //   |<----- start
        // stop ------>|
        //   |<------ stop
        val (state, _) = withState(CountingDownState(2)).withHandler { latch =>
          new WebsocketHandler {
            override val onMessage: OnMessage = {
              case M.String("stop")  => latch.countDown(); sender().close()
              case M.String("start") => sender().send("stop"); latch.countDown()
              case m                 => fail(s"unexpected message: $m")
            }
          }
        }.withScript { ws =>
          ws.send("start")
        }.run()

        assert(state.succeeded, "haven't received echo in 1 second")
      }
    }
  }
}
